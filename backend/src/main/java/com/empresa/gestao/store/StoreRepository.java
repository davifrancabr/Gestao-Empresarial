package com.empresa.gestao.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {

    List<Store> findByActiveTrueOrderByName();

    boolean existsByIdAndActiveTrue(UUID id);
}
