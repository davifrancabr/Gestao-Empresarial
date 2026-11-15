package com.empresa.gestao.store.infrastructure.persistence.adapter;

import com.empresa.gestao.shared.domain.NotFoundException;
import com.empresa.gestao.store.application.dto.StoreResult;
import com.empresa.gestao.store.application.dto.StoreSummary;
import com.empresa.gestao.store.application.port.StoreQueryPort;
import com.empresa.gestao.store.domain.model.Store;
import com.empresa.gestao.store.domain.repository.StoreRepository;
import com.empresa.gestao.store.infrastructure.persistence.entity.StoreJpaEntity;
import com.empresa.gestao.store.infrastructure.persistence.mapper.StorePersistenceMapper;
import com.empresa.gestao.store.infrastructure.persistence.repository.StoreJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class StoreRepositoryAdapter implements StoreRepository, StoreQueryPort {

    private final StoreJpaRepository repository;

    public StoreRepositoryAdapter(StoreJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Store> findById(UUID id) {
        return repository.findById(id).map(StorePersistenceMapper::toDomain);
    }

    @Override
    public List<Store> findByActiveTrueOrderByName() {
        return repository.findByActiveTrueOrderByName().stream()
                .map(StorePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByIdAndActiveTrue(UUID id) {
        return repository.existsByIdAndActiveTrue(id);
    }

    @Override
    public List<Store> findAllByIdIn(Collection<UUID> ids) {
        return repository.findAllById(ids).stream()
                .map(StorePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Store save(Store store) {
        StoreJpaEntity entity = store.id() == null
                ? new StoreJpaEntity()
                : repository.findById(store.id()).orElseGet(StoreJpaEntity::new);
        StorePersistenceMapper.apply(store, entity);
        return StorePersistenceMapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<StoreResult> findStoreById(UUID id) {
        return repository.findById(id).map(StorePersistenceMapper::toDomain).map(this::toResult);
    }

    @Override
    public List<StoreResult> findActiveOrderByName() {
        return repository.findByActiveTrueOrderByName().stream()
                .map(StorePersistenceMapper::toDomain)
                .map(this::toResult)
                .toList();
    }

    @Override
    public boolean existsActive(UUID id) {
        return repository.existsByIdAndActiveTrue(id);
    }

    @Override
    public List<StoreResult> findAllByIds(Collection<UUID> ids) {
        Map<UUID, Store> byId = new LinkedHashMap<>();
        repository.findAllById(ids).forEach(entity ->
                byId.put(entity.getId(), StorePersistenceMapper.toDomain(entity)));
        return ids.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .map(this::toResult)
                .toList();
    }

    @Override
    public List<StoreSummary> findSummariesByIds(Collection<UUID> ids) {
        Map<UUID, Store> byId = new LinkedHashMap<>();
        repository.findAllById(ids).forEach(entity ->
                byId.put(entity.getId(), StorePersistenceMapper.toDomain(entity)));
        return ids.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .map(store -> new StoreSummary(store.id(), store.name()))
                .toList();
    }

    @Override
    public StoreResult getRequired(UUID id) {
        return findStoreById(id).orElseThrow(() -> new NotFoundException("Loja não encontrada."));
    }

    private StoreResult toResult(Store store) {
        return new StoreResult(store.id(), store.name(), store.document(), store.active(),
                store.createdAt(), store.updatedAt());
    }
}
