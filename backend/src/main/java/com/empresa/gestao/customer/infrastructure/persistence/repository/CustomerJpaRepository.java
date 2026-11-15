package com.empresa.gestao.customer.infrastructure.persistence.repository;

import com.empresa.gestao.customer.infrastructure.persistence.entity.CustomerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerJpaRepository extends JpaRepository<CustomerJpaEntity, UUID>,
        JpaSpecificationExecutor<CustomerJpaEntity> {

    Optional<CustomerJpaEntity> findByIdAndStoreId(UUID id, UUID storeId);

    Optional<CustomerJpaEntity> findByIdAndStoreIdAndActiveTrue(UUID id, UUID storeId);

    @Query("""
            select c.id from CustomerJpaEntity c
            where c.storeId = :storeId and c.normalizedName like :term
            """)
    List<UUID> findIdsByName(@Param("storeId") UUID storeId, @Param("term") String term);
}
