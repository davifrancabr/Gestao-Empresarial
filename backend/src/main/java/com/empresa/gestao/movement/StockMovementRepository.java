package com.empresa.gestao.movement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID>,
        JpaSpecificationExecutor<StockMovement> {

    @EntityGraph(attributePaths = {"user", "product"})
    Optional<StockMovement> findByIdAndStoreId(UUID id, UUID storeId);

    @EntityGraph(attributePaths = {"user", "product"})
    List<StockMovement> findTop5ByStoreIdOrderByCreatedAtDesc(UUID storeId);

    long countByStoreId(UUID storeId);

    long countByStoreIdAndType(UUID storeId, MovementType type);

    @EntityGraph(attributePaths = {"user", "product"})
    @Override
    Page<StockMovement> findAll(Specification<StockMovement> specification, Pageable pageable);

    @Query(value = """
            select cast(date_trunc('day', created_at) as date) as day,
                   type as type,
                   count(*) as total
            from stock_movement
            where store_id = :storeId and created_at >= :since
            group by 1, 2
            order by 1
            """, nativeQuery = true)
    List<DailyMovementCount> countDaily(@Param("storeId") UUID storeId, @Param("since") Instant since);

    @Query("""
            select m.product.id as productId,
                   m.productNameSnapshot as productName,
                   sum(m.quantity) as totalQuantity,
                   count(m) as movementCount
            from StockMovement m
            where m.store.id = :storeId
            group by m.product.id, m.productNameSnapshot
            order by sum(m.quantity) desc, m.productNameSnapshot asc
            """)
    List<ProductMovementAggregate> findTopMoved(@Param("storeId") UUID storeId, Pageable pageable);

    @Query("""
            select m.product.id as productId,
                   m.productNameSnapshot as productName,
                   sum(m.quantity) as totalQuantity,
                   count(m) as movementCount
            from StockMovement m
            where m.store.id = :storeId and m.createdAt >= :since
            group by m.product.id, m.productNameSnapshot
            order by sum(m.quantity) desc, m.productNameSnapshot asc
            """)
    List<ProductMovementAggregate> findTopMovedSince(@Param("storeId") UUID storeId,
                                                     @Param("since") Instant since,
                                                     Pageable pageable);

    interface DailyMovementCount {
        java.sql.Date getDay();

        String getType();

        Long getTotal();
    }

    interface ProductMovementAggregate {
        UUID getProductId();

        String getProductName();

        Long getTotalQuantity();

        Long getMovementCount();
    }
}
