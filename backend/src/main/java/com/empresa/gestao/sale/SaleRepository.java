package com.empresa.gestao.sale;

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

public interface SaleRepository extends JpaRepository<Sale, UUID>, JpaSpecificationExecutor<Sale> {

    @EntityGraph(attributePaths = {"customer", "user", "items", "items.product"})
    @Query("select s from Sale s where s.id = :id and s.store.id = :storeId")
    Optional<Sale> findDetailByIdAndStoreId(@Param("id") UUID id, @Param("storeId") UUID storeId);

    @EntityGraph(attributePaths = {"customer", "user", "items"})
    @Override
    Page<Sale> findAll(Specification<Sale> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "user", "items"})
    @Query("select s from Sale s where s.store.id = :storeId and s.customer.id = :customerId order by s.createdAt desc")
    Page<Sale> findByCustomer(@Param("storeId") UUID storeId,
                              @Param("customerId") UUID customerId,
                              Pageable pageable);

    long countByStoreId(UUID storeId);

    @Query("select coalesce(sum(s.total), 0) from Sale s where s.store.id = :storeId and s.status = :status")
    java.math.BigDecimal sumRevenue(@Param("storeId") UUID storeId,
                                    @Param("status") SaleStatus status);

    @Query("""
            select coalesce(sum(s.total), 0) from Sale s
            where s.store.id = :storeId and s.status = :status
              and s.createdAt >= :from
              and s.createdAt < :to
            """)
    java.math.BigDecimal sumRevenue(@Param("storeId") UUID storeId,
                                    @Param("status") SaleStatus status,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to);
}
