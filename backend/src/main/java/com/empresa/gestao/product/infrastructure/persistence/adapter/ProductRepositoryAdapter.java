package com.empresa.gestao.product.infrastructure.persistence.adapter;

import com.empresa.gestao.product.domain.repository.ProductFilter;
import com.empresa.gestao.product.application.dto.ProductInfo;
import com.empresa.gestao.product.application.port.ProductQueryPort;
import com.empresa.gestao.product.domain.model.Product;
import com.empresa.gestao.product.domain.repository.ProductRepository;
import com.empresa.gestao.product.infrastructure.persistence.entity.ProductJpaEntity;
import com.empresa.gestao.product.infrastructure.persistence.mapper.ProductPersistenceMapper;
import com.empresa.gestao.product.infrastructure.persistence.repository.ProductJpaRepository;
import com.empresa.gestao.product.infrastructure.persistence.repository.ProductSpecifications;
import com.empresa.gestao.shared.domain.query.PageQuery;
import com.empresa.gestao.shared.domain.query.PageResult;
import com.empresa.gestao.shared.infrastructure.persistence.JpaPageMapper;
import com.empresa.gestao.shared.infrastructure.persistence.PageableFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProductRepositoryAdapter implements ProductRepository, ProductQueryPort {

    private final ProductJpaRepository repository;

    public ProductRepositoryAdapter(ProductJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return repository.findById(id).map(ProductPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Product> findByIdAndStoreId(UUID id, UUID storeId) {
        return repository.findByIdAndStoreId(id, storeId).map(ProductPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Product> findByIdAndStoreIdAndActiveTrue(UUID id, UUID storeId) {
        return repository.findByIdAndStoreIdAndActiveTrue(id, storeId)
                .map(ProductPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByNormalizedName(UUID storeId, String normalizedName, UUID excludeId) {
        return excludeId == null
                ? repository.existsByStoreIdAndNormalizedNameAndActiveTrue(storeId, normalizedName)
                : repository.existsByStoreIdAndNormalizedNameAndActiveTrueAndIdNot(
                        storeId, normalizedName, excludeId);
    }

    @Override
    public boolean existsBySku(UUID storeId, String sku, UUID excludeId) {
        return excludeId == null
                ? repository.existsByStoreIdAndSkuIgnoreCaseAndActiveTrue(storeId, sku)
                : repository.existsByStoreIdAndSkuIgnoreCaseAndActiveTrueAndIdNot(storeId, sku, excludeId);
    }

    @Override
    public Product save(Product product) {
        return ProductPersistenceMapper.toDomain(repository.save(entityFor(product)));
    }

    @Override
    public Product saveAndFlush(Product product) {
        return ProductPersistenceMapper.toDomain(repository.saveAndFlush(entityFor(product)));
    }

    @Override
    public void flush() {
        repository.flush();
    }

    @Override
    public PageResult<Product> search(ProductFilter filter, PageQuery pageQuery) {
        return JpaPageMapper.toPageResult(
                repository.findAll(ProductSpecifications.filtered(filter),
                        PageableFactory.toPageable(pageQuery)),
                ProductPersistenceMapper::toDomain);
    }

    @Override
    public List<String> findCategories(UUID storeId) {
        return repository.findCategories(storeId);
    }

    @Override
    public List<Product> findLowStock(UUID storeId, int limit) {
        return repository.findLowStock(storeId, PageRequest.of(0, limit)).stream()
                .map(ProductPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Product> findOutOfStock(UUID storeId, int limit) {
        return repository.findOutOfStock(storeId, PageRequest.of(0, limit)).stream()
                .map(ProductPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Product> findLowTurnover(UUID storeId, Instant since, int limit) {
        return repository.findLowTurnover(storeId, since, PageRequest.of(0, limit)).stream()
                .map(ProductPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public long countActive(UUID storeId) {
        return repository.countByStoreIdAndActiveTrue(storeId);
    }

    @Override
    public long countByQuantity(UUID storeId, int quantity) {
        return repository.countByStoreIdAndActiveTrueAndQuantity(storeId, quantity);
    }

    @Override
    public long countLowStock(UUID storeId) {
        return repository.countLowStock(storeId);
    }

    @Override
    public long sumQuantity(UUID storeId) {
        return repository.sumQuantity(storeId);
    }

    @Override
    public List<UUID> findIdsByNameOrSku(UUID storeId, String term) {
        return repository.findIdsByNameOrSku(storeId, term);
    }

    @Override
    public Map<UUID, String> findSkusByIds(Collection<UUID> ids) {
        Map<UUID, String> skus = new HashMap<>();
        repository.findSkuRows(ids).forEach(row -> skus.put((UUID) row[0], (String) row[1]));
        return skus;
    }

    @Override
    public Optional<ProductInfo> findActive(UUID storeId, UUID id) {
        return repository.findByIdAndStoreIdAndActiveTrue(id, storeId)
                .map(ProductPersistenceMapper::toDomain)
                .map(product -> new ProductInfo(product.id(), product.storeId(), product.name(),
                        product.sku(), product.quantity(), product.minimum(), product.unit(),
                        product.active(), product.version()));
    }

    private ProductJpaEntity entityFor(Product product) {
        ProductJpaEntity entity = product.id() == null
                ? new ProductJpaEntity()
                : repository.findById(product.id()).orElseGet(ProductJpaEntity::new);
        ProductPersistenceMapper.apply(product, entity);
        return entity;
    }
}
