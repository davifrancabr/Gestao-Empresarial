package com.empresa.gestao.sale.infrastructure.persistence.repository;

import com.empresa.gestao.sale.infrastructure.persistence.entity.SaleJpaEntity;
import com.empresa.gestao.shared.domain.model.SaleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SaleJpaRepository extends JpaRepository<SaleJpaEntity, UUID>,
        JpaSpecificationExecutor<SaleJpaEntity> {

    @EntityGraph(attributePaths = {"items"})
    @Query("select s from SaleJpaEntity s where s.id = :id and s.storeId = :storeId")
    Optional<SaleJpaEntity> findDetailByIdAndStoreId(@Param("id") UUID id,
                                                     @Param("storeId") UUID storeId);

    @EntityGraph(attributePaths = {"items"})
    @Override
    Page<SaleJpaEntity> findAll(Specification<SaleJpaEntity> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"items"})
    @Query("""
            select s from SaleJpaEntity s
            where s.storeId = :storeId and s.customerId = :customerId
            order by s.createdAt desc
            """)
    Page<SaleJpaEntity> findByCustomer(@Param("storeId") UUID storeId,
                                       @Param("customerId") UUID customerId,
                                       Pageable pageable);

    long countByStoreId(UUID storeId);

    @Query("select coalesce(sum(s.total), 0) from SaleJpaEntity s where s.storeId = :storeId and s.status = :status")
    java.math.BigDecimal sumRevenue(@Param("storeId") UUID storeId,
                                    @Param("status") SaleStatus status);

    @Query("""
            select coalesce(sum(s.total), 0) from SaleJpaEntity s
            where s.storeId = :storeId and s.status = :status
              and s.createdAt >= :from
              and s.createdAt < :to
            """)
    java.math.BigDecimal sumRevenue(@Param("storeId") UUID storeId,
                                    @Param("status") SaleStatus status,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to);
}
