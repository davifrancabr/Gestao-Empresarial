package com.empresa.gestao.store.domain.repository;

import com.empresa.gestao.store.domain.model.Store;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreRepository {

    Optional<Store> findById(UUID id);

    List<Store> findByActiveTrueOrderByName();

    boolean existsByIdAndActiveTrue(UUID id);

    List<Store> findAllByIdIn(Collection<UUID> ids);

    Store save(Store store);
}
