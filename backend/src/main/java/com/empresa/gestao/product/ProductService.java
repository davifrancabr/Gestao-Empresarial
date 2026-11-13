package com.empresa.gestao.product;

import com.empresa.gestao.common.dto.PageResponse;
import com.empresa.gestao.common.exception.BusinessRuleException;
import com.empresa.gestao.common.exception.ConflictException;
import com.empresa.gestao.common.exception.ResourceNotFoundException;
import com.empresa.gestao.common.util.PageUtils;
import com.empresa.gestao.common.util.TextNormalizer;
import com.empresa.gestao.movement.MovementService;
import com.empresa.gestao.movement.MovementType;
import com.empresa.gestao.movement.dto.MovementRequest;
import com.empresa.gestao.product.dto.ProductCreateRequest;
import com.empresa.gestao.product.dto.ProductResponse;
import com.empresa.gestao.product.dto.ProductUpdateRequest;
import com.empresa.gestao.store.Store;
import com.empresa.gestao.store.StoreRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final MovementService movementService;
    private final ProductMapper mapper;

    public ProductService(ProductRepository productRepository, StoreRepository storeRepository,
                          MovementService movementService, ProductMapper mapper) {
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.movementService = movementService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(UUID storeId, String search, String category,
                                              ProductStatus status, String sort,
                                              Integer page, Integer size, boolean includeInactive) {
        String term = MovementService.searchTerm(search);
        String cleanCategory = TextNormalizer.cleanText(category);

        Specification<Product> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("store").get("id"), storeId));
            if (!includeInactive) {
                predicates.add(cb.isTrue(root.get("active")));
            }
            if (term != null) {
                String skuTerm = term;
                predicates.add(cb.or(
                        cb.like(root.get("normalizedName"), term),
                        cb.like(cb.lower(cb.coalesce(root.get("sku"), "")), skuTerm)));
            }
            if (cleanCategory != null) {
                predicates.add(cb.equal(root.get("category"), cleanCategory));
            }
            if (status != null) {
                switch (status) {
                    case NORMAL -> predicates.add(cb.greaterThan(root.get("quantity"), root.get("minimum")));
                    case LOW -> predicates.add(cb.and(
                            cb.greaterThan(root.get("quantity"), 0),
                            cb.lessThanOrEqualTo(root.get("quantity"), root.get("minimum"))));
                    case OUT_OF_STOCK -> predicates.add(cb.equal(root.get("quantity"), 0));
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Pageable pageable = PageUtils.create(page, size, sort, Map.of(
                "name", "normalizedName",
                "quantity", "quantity",
                "minimum", "minimum",
                "category", "category",
                "createdAt", "createdAt",
                "updatedAt", "updatedAt"
        ), "name,asc");

        Page<Product> result = productRepository.findAll(specification, pageable);
        return PageResponse.from(result, mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(UUID storeId, UUID id) {
        return mapper.toResponse(findProduct(storeId, id));
    }

    @Transactional(readOnly = true)
    public List<String> categories(UUID storeId) {
        return productRepository.findCategories(storeId);
    }

    @Transactional
    public ProductResponse create(UUID storeId, UUID userId, ProductCreateRequest request) {
        String name = cleanName(request.name());
        String normalizedName = TextNormalizer.normalizeName(name);
        ensureUnique(storeId, normalizedName, cleanSku(request.sku()), null);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Loja não encontrada."));

        Product product = new Product();
        product.setStore(store);
        product.setName(name);
        product.setNormalizedName(normalizedName);
        product.setSku(cleanSku(request.sku()));
        product.setCategory(cleanCategory(request.category()));
        product.setDescription(TextNormalizer.cleanText(request.description()));
        product.setQuantity(0);
        product.setMinimum(request.minimum());
        product.setUnit(cleanUnit(request.unit()));
        product.setActive(true);
        product = productRepository.save(product);

        int initial = request.quantity() == null ? 0 : request.quantity();
        if (initial > 0) {
            movementService.record(storeId, userId, false, new MovementRequest(
                    product.getId(), MovementType.ENTRY, initial, "Saldo inicial",
                    "Cadastro inicial do produto"));
        }

        // O saldo inicial altera a versão otimista do produto. Sem o flush a
        // resposta devolveria a versão antiga e a primeira edição falharia com 409.
        productRepository.flush();
        return mapper.toResponse(productRepository.findById(product.getId()).orElseThrow());
    }

    @Transactional
    public ProductResponse update(UUID storeId, UUID id, ProductUpdateRequest request) {
        Product product = findActiveProduct(storeId, id);

        if (request.version() != null && product.getVersion() != null
                && !Objects.equals(request.version(), product.getVersion())) {
            throw new ConflictException("O produto foi alterado por outra operação. Recarregue os dados.");
        }

        String name = cleanName(request.name());
        String normalizedName = TextNormalizer.normalizeName(name);
        String sku = cleanSku(request.sku());
        ensureUnique(storeId, normalizedName, sku, id);

        product.setName(name);
        product.setNormalizedName(normalizedName);
        product.setSku(sku);
        product.setCategory(cleanCategory(request.category()));
        product.setDescription(TextNormalizer.cleanText(request.description()));
        product.setMinimum(request.minimum());
        product.setUnit(cleanUnit(request.unit()));

        // Flush para que a resposta carregue a versão otimista já incrementada.
        productRepository.save(product);
        productRepository.flush();
        return mapper.toResponse(product);
    }

    @Transactional
    public void archive(UUID storeId, UUID id) {
        Product product = findActiveProduct(storeId, id);
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> lowStock(UUID storeId, int limit) {
        return productRepository.findLowStock(storeId,
                        PageUtils.create(0, limit, null, Map.of("name", "name"), "name,asc"))
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> outOfStock(UUID storeId, int limit) {
        return productRepository.findOutOfStock(storeId,
                        PageUtils.create(0, limit, null, Map.of("name", "name"), "name,asc"))
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> lowTurnover(UUID storeId, int days, int limit) {
        return productRepository.findLowTurnover(storeId,
                        java.time.Instant.now().minus(days, java.time.temporal.ChronoUnit.DAYS),
                        PageUtils.create(0, limit, null, Map.of("name", "name"), "name,asc"))
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Product findActiveProduct(UUID storeId, UUID id) {
        return productRepository.findByIdAndStoreIdAndActiveTrue(id, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado ou arquivado."));
    }

    private Product findProduct(UUID storeId, UUID id) {
        return productRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado."));
    }

    private void ensureUnique(UUID storeId, String normalizedName, String sku, UUID currentId) {
        boolean duplicatedName = currentId == null
                ? productRepository.existsByStoreIdAndNormalizedNameAndActiveTrue(storeId, normalizedName)
                : productRepository.existsByStoreIdAndNormalizedNameAndActiveTrueAndIdNot(
                        storeId, normalizedName, currentId);
        if (duplicatedName) {
            throw new ConflictException("Já existe um produto com esse nome nesta loja.");
        }
        if (sku != null) {
            boolean duplicatedSku = currentId == null
                    ? productRepository.existsByStoreIdAndSkuIgnoreCaseAndActiveTrue(storeId, sku)
                    : productRepository.existsByStoreIdAndSkuIgnoreCaseAndActiveTrueAndIdNot(storeId, sku, currentId);
            if (duplicatedSku) {
                throw new ConflictException("Já existe um produto com esse SKU nesta loja.");
            }
        }
    }

    private String cleanName(String value) {
        String name = TextNormalizer.cleanText(value);
        if (name == null) {
            throw new BusinessRuleException("Informe o nome do produto.");
        }
        if (name.length() > 80) {
            throw new BusinessRuleException("O nome deve ter no máximo 80 caracteres.");
        }
        return name;
    }

    private String cleanSku(String value) {
        String sku = TextNormalizer.cleanSku(value);
        if (sku != null && sku.length() > 40) {
            throw new BusinessRuleException("O SKU deve ter no máximo 40 caracteres.");
        }
        return sku;
    }

    private String cleanCategory(String value) {
        String category = TextNormalizer.cleanText(value);
        return category == null ? "Geral" : category;
    }

    private String cleanUnit(String value) {
        String unit = TextNormalizer.cleanText(value);
        return unit == null ? "un" : unit;
    }

    @Transactional(readOnly = true)
    public long countActive(UUID storeId) {
        return productRepository.countByStoreIdAndActiveTrue(storeId);
    }
}
