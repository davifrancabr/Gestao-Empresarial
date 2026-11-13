package com.empresa.gestao.movement;

import com.empresa.gestao.common.dto.PageResponse;
import com.empresa.gestao.common.exception.BusinessRuleException;
import com.empresa.gestao.common.exception.ForbiddenException;
import com.empresa.gestao.common.exception.ResourceNotFoundException;
import com.empresa.gestao.common.util.PageUtils;
import com.empresa.gestao.common.util.TextNormalizer;
import com.empresa.gestao.movement.dto.MovementRequest;
import com.empresa.gestao.movement.dto.MovementResponse;
import com.empresa.gestao.product.Product;
import com.empresa.gestao.product.ProductRepository;
import com.empresa.gestao.store.Store;
import com.empresa.gestao.store.StoreRepository;
import com.empresa.gestao.user.User;
import com.empresa.gestao.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Service
public class MovementService {

    private final StockMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final MovementMapper mapper;

    public MovementService(StockMovementRepository movementRepository, ProductRepository productRepository,
                           StoreRepository storeRepository, UserRepository userRepository,
                           MovementMapper mapper) {
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Transactional
    public MovementResponse record(UUID storeId, UUID userId, boolean allowAdjustment, MovementRequest request) {
        return recordAt(storeId, userId, allowAdjustment, request, null);
    }

    /**
     * Registra uma movimentação com data controlada. Usado apenas pela carga de
     * dados de demonstração para produzir histórico coerente.
     */
    @Transactional
    public MovementResponse recordAt(UUID storeId, UUID userId, boolean allowAdjustment,
                                     MovementRequest request, Instant occurredAt) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Loja não encontrada."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        Product product = productRepository.findByIdAndStoreIdAndActiveTrue(request.productId(), storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado ou arquivado."));

        return mapper.toResponse(apply(store, user, product, allowAdjustment, request, occurredAt));
    }

    /**
     * Aplica a movimentação e atualiza o saldo na mesma transação.
     */
    @Transactional
    public StockMovement apply(Store store, User user, Product product, boolean allowAdjustment,
                               MovementRequest request, Instant occurredAt) {
        int previous = product.getQuantity();
        int requested = request.quantity();
        int resulting;
        int recordedQuantity;

        switch (request.type()) {
            case ENTRY -> {
                if (requested <= 0) {
                    throw new BusinessRuleException("Informe uma quantidade inteira maior que zero.");
                }
                resulting = previous + requested;
                recordedQuantity = requested;
            }
            case EXIT -> {
                if (requested <= 0) {
                    throw new BusinessRuleException("Informe uma quantidade inteira maior que zero.");
                }
                if (requested > previous) {
                    throw new BusinessRuleException(
                            "Saída indisponível. Há apenas " + previous
                                    + (previous == 1 ? " unidade" : " unidades") + " em estoque.");
                }
                resulting = previous - requested;
                recordedQuantity = requested;
            }
            case ADJUSTMENT -> {
                if (!allowAdjustment) {
                    throw new ForbiddenException("Apenas administradores e gerentes podem realizar ajustes.");
                }
                if (requested < 0) {
                    throw new BusinessRuleException("O saldo ajustado não pode ser negativo.");
                }
                if (requested == previous) {
                    throw new BusinessRuleException("O saldo informado é igual ao saldo atual.");
                }
                resulting = requested;
                recordedQuantity = Math.abs(resulting - previous);
            }
            default -> throw new BusinessRuleException("Tipo de movimentação inválido.");
        }

        String reason = TextNormalizer.cleanText(request.reason());
        if (reason == null) {
            throw new BusinessRuleException("Informe o motivo da movimentação.");
        }

        product.setQuantity(resulting);
        productRepository.save(product);

        StockMovement movement = new StockMovement();
        movement.setStore(store);
        movement.setProduct(product);
        movement.setProductNameSnapshot(product.getName());
        movement.setType(request.type());
        movement.setQuantity(recordedQuantity);
        movement.setPreviousQuantity(previous);
        movement.setResultingQuantity(resulting);
        movement.setReason(reason);
        movement.setObservation(TextNormalizer.cleanText(request.observation()));
        movement.setUser(user);
        if (occurredAt != null) {
            movement.setCreatedAt(occurredAt);
        }
        return movementRepository.save(movement);
    }

    @Transactional(readOnly = true)
    public PageResponse<MovementResponse> search(UUID storeId, UUID productId, MovementType type,
                                                 LocalDate from, LocalDate to, String search,
                                                 Integer page, Integer size, String sort) {
        Instant fromInstant = from == null ? null : from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to == null ? null : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        String term = searchTerm(search);

        Pageable pageable = PageUtils.create(page, size, sort,
                Map.of("createdAt", "createdAt"), "createdAt,desc");

        return PageResponse.from(
                movementRepository.findAll(
                        MovementSpecifications.filtered(storeId, productId, type, fromInstant, toInstant, term),
                        pageable),
                mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public MovementResponse get(UUID storeId, UUID id) {
        StockMovement movement = movementRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimentação não encontrada."));
        return mapper.toResponse(movement);
    }

    @Transactional(readOnly = true)
    public MovementResponse toResponse(StockMovement movement) {
        return mapper.toResponse(movement);
    }

    public static String searchTerm(String search) {
        String cleaned = TextNormalizer.normalizeName(search);
        if (cleaned.isEmpty()) {
            return null;
        }
        return "%" + cleaned.replace("%", "").replace("_", "") + "%";
    }
}
