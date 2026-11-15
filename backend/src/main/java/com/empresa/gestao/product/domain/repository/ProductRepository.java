package com.empresa.gestao.product.domain.repository;

import com.empresa.gestao.product.domain.repository.ProductFilter;
import com.empresa.gestao.product.domain.model.Product;
import com.empresa.gestao.shared.domain.query.PageQuery;
import com.empresa.gestao.shared.domain.query.PageResult;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    Optional<Product> findById(UUID id);

    Optional<Product> findByIdAndStoreId(UUID id, UUID storeId);

    Optional<Product> findByIdAndStoreIdAndActiveTrue(UUID id, UUID storeId);

    boolean existsByNormalizedName(UUID storeId, String normalizedName, UUID excludeId);

    boolean existsBySku(UUID storeId, String sku, UUID excludeId);

    Product save(Product product);

    Product saveAndFlush(Product product);

    void flush();

    PageResult<Product> search(ProductFilter filter, PageQuery pageQuery);

    List<String> findCategories(UUID storeId);

    List<Product> findLowStock(UUID storeId, int limit);

    List<Product> findOutOfStock(UUID storeId, int limit);

    List<Product> findLowTurnover(UUID storeId, Instant since, int limit);

    long countActive(UUID storeId);

    long countByQuantity(UUID storeId, int quantity);

    long countLowStock(UUID storeId);

    long sumQuantity(UUID storeId);

    List<UUID> findIdsByNameOrSku(UUID storeId, String term);

    Map<UUID, String> findSkusByIds(Collection<UUID> ids);
}
