package com.empresa.gestao.sale;

import com.empresa.gestao.common.dto.PageResponse;
import com.empresa.gestao.common.exception.BusinessRuleException;
import com.empresa.gestao.common.exception.ResourceNotFoundException;
import com.empresa.gestao.common.util.PageUtils;
import com.empresa.gestao.customer.Customer;
import com.empresa.gestao.customer.CustomerService;
import com.empresa.gestao.movement.MovementService;
import com.empresa.gestao.movement.MovementType;
import com.empresa.gestao.movement.dto.MovementRequest;
import com.empresa.gestao.product.Product;
import com.empresa.gestao.product.ProductRepository;
import com.empresa.gestao.sale.dto.SaleCreateRequest;
import com.empresa.gestao.sale.dto.SaleItemRequest;
import com.empresa.gestao.sale.dto.SaleResponse;
import com.empresa.gestao.sale.dto.SaleSummaryResponse;
import com.empresa.gestao.store.Store;
import com.empresa.gestao.store.StoreRepository;
import com.empresa.gestao.user.User;
import com.empresa.gestao.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final CustomerService customerService;
    private final MovementService movementService;
    private final SaleMapper mapper;

    public SaleService(SaleRepository saleRepository, ProductRepository productRepository,
                       StoreRepository storeRepository, UserRepository userRepository,
                       CustomerService customerService, MovementService movementService,
                       SaleMapper mapper) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.customerService = customerService;
        this.movementService = movementService;
        this.mapper = mapper;
    }

    @Transactional
    public SaleResponse create(UUID storeId, UUID userId, SaleCreateRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Loja não encontrada."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        Customer customer = request.customerId() == null ? null
                : customerService.findActive(storeId, request.customerId());

        Set<UUID> seenProducts = new HashSet<>();
        List<Product> products = new ArrayList<>();
        List<SaleItemRequest> items = new ArrayList<>();

        Sale sale = new Sale();
        sale.setStore(store);
        sale.setCustomer(customer);
        sale.setUser(user);
        sale.setStatus(SaleStatus.CONCLUIDA);

        BigDecimal total = BigDecimal.ZERO;

        for (SaleItemRequest itemRequest : request.items()) {
            if (!seenProducts.add(itemRequest.productId())) {
                throw new BusinessRuleException("O mesmo produto foi informado mais de uma vez na venda.");
            }
            Product product = productRepository.findByIdAndStoreIdAndActiveTrue(itemRequest.productId(), storeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado ou arquivado."));
            if (itemRequest.quantity() > product.getQuantity()) {
                throw new BusinessRuleException(
                        "Estoque insuficiente para " + product.getName() + ". Disponível: "
                                + product.getQuantity() + ".");
            }

            BigDecimal subtotal = null;
            if (itemRequest.unitPrice() != null) {
                subtotal = itemRequest.unitPrice()
                        .multiply(BigDecimal.valueOf(itemRequest.quantity()))
                        .setScale(2, RoundingMode.HALF_UP);
                total = total.add(subtotal);
            }

            SaleItem saleItem = new SaleItem();
            saleItem.setProduct(product);
            saleItem.setSale(sale);
            saleItem.setProductNameSnapshot(product.getName());
            saleItem.setQuantity(itemRequest.quantity());
            saleItem.setUnitPrice(itemRequest.unitPrice());
            saleItem.setSubtotal(subtotal);
            sale.addItem(saleItem);

            products.add(product);
            items.add(itemRequest);
        }

        sale.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        sale = saleRepository.save(sale);

        for (int index = 0; index < products.size(); index++) {
            Product product = products.get(index);
            SaleItemRequest item = items.get(index);
            movementService.apply(store, user, product, false, new MovementRequest(
                    product.getId(),
                    MovementType.EXIT,
                    item.quantity(),
                    "Venda",
                    "Venda " + sale.getId()
            ), null);
        }

        return mapper.toResponse(sale);
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleSummaryResponse> list(UUID storeId, UUID customerId, LocalDate from, LocalDate to,
                                                  String search, Integer page, Integer size, String sort) {
        Instant fromInstant = from == null ? null : from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to == null ? null : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Pageable pageable = PageUtils.create(page, size, sort, Map.of(
                "createdAt", "createdAt",
                "total", "total"
        ), "createdAt,desc");
        return PageResponse.from(
                saleRepository.findAll(
                        SaleSpecifications.filtered(storeId, customerId, fromInstant, toInstant,
                                MovementService.searchTerm(search)),
                        pageable),
                mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public SaleResponse get(UUID storeId, UUID id) {
        Sale sale = saleRepository.findDetailByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Venda não encontrada."));
        return mapper.toResponse(sale);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalRevenue(UUID storeId) {
        return saleRepository.sumRevenue(storeId, SaleStatus.CONCLUIDA);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalRevenue(UUID storeId, LocalDate from, LocalDate to) {
        Instant fromInstant = from == null
                ? Instant.EPOCH
                : from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to == null
                ? Instant.parse("9999-12-31T00:00:00Z")
                : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return saleRepository.sumRevenue(storeId, SaleStatus.CONCLUIDA, fromInstant, toInstant);
    }

    @Transactional(readOnly = true)
    public long count(UUID storeId) {
        return saleRepository.countByStoreId(storeId);
    }
}
