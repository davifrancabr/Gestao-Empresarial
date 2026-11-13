package com.empresa.gestao.product;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByIdAndStoreId(UUID id, UUID storeId);

    Optional<Product> findByIdAndStoreIdAndActiveTrue(UUID id, UUID storeId);

    boolean existsByStoreIdAndNormalizedNameAndActiveTrue(UUID storeId, String normalizedName);

    boolean existsByStoreIdAndNormalizedNameAndActiveTrueAndIdNot(UUID storeId, String normalizedName, UUID id);

    boolean existsByStoreIdAndSkuIgnoreCaseAndActiveTrue(UUID storeId, String sku);

    boolean existsByStoreIdAndSkuIgnoreCaseAndActiveTrueAndIdNot(UUID storeId, String sku, UUID id);

    long countByStoreIdAndActiveTrue(UUID storeId);

    long countByStoreIdAndActiveTrueAndQuantity(UUID storeId, int quantity);

    @Query("""
            select count(p) from Product p
            where p.store.id = :storeId and p.active = true
              and p.quantity > 0 and p.quantity <= p.minimum
            """)
    long countLowStock(@Param("storeId") UUID storeId);

    @Query("select coalesce(sum(p.quantity), 0) from Product p where p.store.id = :storeId and p.active = true")
    long sumQuantity(@Param("storeId") UUID storeId);

    @Query("""
            select distinct p.category from Product p
            where p.store.id = :storeId and p.active = true
            order by p.category
            """)
    List<String> findCategories(@Param("storeId") UUID storeId);

    @Query("""
            select p from Product p
            where p.store.id = :storeId and p.active = true
              and p.quantity > 0 and p.quantity <= p.minimum
            order by p.quantity asc, p.name asc
            """)
    List<Product> findLowStock(@Param("storeId") UUID storeId, Pageable pageable);

    @Query("""
            select p from Product p
            where p.store.id = :storeId and p.active = true and p.quantity = 0
            order by p.name asc
            """)
    List<Product> findOutOfStock(@Param("storeId") UUID storeId, Pageable pageable);

    @Query("""
            select p from Product p
            where p.store.id = :storeId and p.active = true
              and not exists (
                select 1 from StockMovement m
                where m.product.id = p.id and m.createdAt >= :since
              )
            order by p.quantity desc, p.name asc
            """)
    List<Product> findLowTurnover(@Param("storeId") UUID storeId, @Param("since") Instant since,
                                  Pageable pageable);
}
