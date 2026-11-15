package com.empresa.gestao.product.infrastructure.persistence.repository;

import com.empresa.gestao.product.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID>,
        JpaSpecificationExecutor<ProductJpaEntity> {

    Optional<ProductJpaEntity> findByIdAndStoreId(UUID id, UUID storeId);

    Optional<ProductJpaEntity> findByIdAndStoreIdAndActiveTrue(UUID id, UUID storeId);

    boolean existsByStoreIdAndNormalizedNameAndActiveTrue(UUID storeId, String normalizedName);

    boolean existsByStoreIdAndNormalizedNameAndActiveTrueAndIdNot(UUID storeId, String normalizedName, UUID id);

    boolean existsByStoreIdAndSkuIgnoreCaseAndActiveTrue(UUID storeId, String sku);

    boolean existsByStoreIdAndSkuIgnoreCaseAndActiveTrueAndIdNot(UUID storeId, String sku, UUID id);

    long countByStoreIdAndActiveTrue(UUID storeId);

    long countByStoreIdAndActiveTrueAndQuantity(UUID storeId, int quantity);

    @Query("""
            select count(p) from ProductJpaEntity p
            where p.storeId = :storeId and p.active = true
              and p.quantity > 0 and p.quantity <= p.minimum
            """)
    long countLowStock(@Param("storeId") UUID storeId);

    @Query("select coalesce(sum(p.quantity), 0) from ProductJpaEntity p where p.storeId = :storeId and p.active = true")
    long sumQuantity(@Param("storeId") UUID storeId);

    @Query("""
            select distinct p.category from ProductJpaEntity p
            where p.storeId = :storeId and p.active = true
            order by p.category
            """)
    List<String> findCategories(@Param("storeId") UUID storeId);

    @Query("""
            select p from ProductJpaEntity p
            where p.storeId = :storeId and p.active = true
              and p.quantity > 0 and p.quantity <= p.minimum
            order by p.quantity asc, p.name asc
            """)
    List<ProductJpaEntity> findLowStock(@Param("storeId") UUID storeId, Pageable pageable);

    @Query("""
            select p from ProductJpaEntity p
            where p.storeId = :storeId and p.active = true and p.quantity = 0
            order by p.name asc
            """)
    List<ProductJpaEntity> findOutOfStock(@Param("storeId") UUID storeId, Pageable pageable);

    @Query("""
            select p from ProductJpaEntity p
            where p.storeId = :storeId and p.active = true
              and not exists (
                select 1 from StockMovementJpaEntity m
                where m.productId = p.id and m.createdAt >= :since
              )
            order by p.quantity desc, p.name asc
            """)
    List<ProductJpaEntity> findLowTurnover(@Param("storeId") UUID storeId,
                                           @Param("since") Instant since,
                                           Pageable pageable);

    @Query("""
            select p.id from ProductJpaEntity p
            where p.storeId = :storeId
              and (p.normalizedName like :term or lower(coalesce(p.sku, '')) like :term)
            """)
    List<UUID> findIdsByNameOrSku(@Param("storeId") UUID storeId, @Param("term") String term);

    @Query("select p.id, p.sku from ProductJpaEntity p where p.id in :ids")
    List<Object[]> findSkuRows(@Param("ids") Collection<UUID> ids);
}
