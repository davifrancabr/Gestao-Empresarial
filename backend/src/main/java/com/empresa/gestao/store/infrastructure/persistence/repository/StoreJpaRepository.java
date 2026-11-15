package com.empresa.gestao.store.infrastructure.persistence.repository;

import com.empresa.gestao.store.infrastructure.persistence.entity.StoreJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StoreJpaRepository extends JpaRepository<StoreJpaEntity, UUID> {

    List<StoreJpaEntity> findByActiveTrueOrderByName();

    boolean existsByIdAndActiveTrue(UUID id);
}
