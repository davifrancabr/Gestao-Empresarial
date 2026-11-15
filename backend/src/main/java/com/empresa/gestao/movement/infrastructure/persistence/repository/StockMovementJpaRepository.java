package com.empresa.gestao.movement.infrastructure.persistence.repository;

import com.empresa.gestao.movement.infrastructure.persistence.entity.StockMovementJpaEntity;
import com.empresa.gestao.shared.domain.model.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockMovementJpaRepository extends JpaRepository<StockMovementJpaEntity, UUID>,
        JpaSpecificationExecutor<StockMovementJpaEntity> {

    Optional<StockMovementJpaEntity> findByIdAndStoreId(UUID id, UUID storeId);

    List<StockMovementJpaEntity> findByStoreIdOrderByCreatedAtDesc(UUID storeId, Pageable pageable);

    long countByStoreId(UUID storeId);

    long countByStoreIdAndType(UUID storeId, MovementType type);

    @Query(value = """
            select cast(date_trunc('day', created_at) as date) as day,
                   type as type,
                   count(*) as total
            from stock_movement
            where store_id = :storeId and created_at >= :since
            group by 1, 2
            order by 1
            """, nativeQuery = true)
    List<DailyMovementCountRow> countDaily(@Param("storeId") UUID storeId, @Param("since") Instant since);

    @Query("""
            select m.productId as productId,
                   m.productNameSnapshot as productName,
                   sum(m.quantity) as totalQuantity,
                   count(m) as movementCount
            from StockMovementJpaEntity m
            where m.storeId = :storeId
            group by m.productId, m.productNameSnapshot
            order by sum(m.quantity) desc, m.productNameSnapshot asc
            """)
    List<ProductMovementAggregateRow> findTopMoved(@Param("storeId") UUID storeId, Pageable pageable);

    @Query("""
            select m.productId as productId,
                   m.productNameSnapshot as productName,
                   sum(m.quantity) as totalQuantity,
                   count(m) as movementCount
            from StockMovementJpaEntity m
            where m.storeId = :storeId and m.createdAt >= :since
            group by m.productId, m.productNameSnapshot
            order by sum(m.quantity) desc, m.productNameSnapshot asc
            """)
    List<ProductMovementAggregateRow> findTopMovedSince(@Param("storeId") UUID storeId,
                                                        @Param("since") Instant since,
                                                        Pageable pageable);

    interface DailyMovementCountRow {
        java.sql.Date getDay();

        String getType();

        Long getTotal();
    }

    interface ProductMovementAggregateRow {
        UUID getProductId();

        String getProductName();

        Long getTotalQuantity();

        Long getMovementCount();
    }
}
