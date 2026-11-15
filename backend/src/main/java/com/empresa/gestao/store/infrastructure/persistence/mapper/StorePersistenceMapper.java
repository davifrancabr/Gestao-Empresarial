package com.empresa.gestao.store.infrastructure.persistence.mapper;

import com.empresa.gestao.store.domain.model.Store;
import com.empresa.gestao.store.infrastructure.persistence.entity.StoreJpaEntity;

public final class StorePersistenceMapper {

    private StorePersistenceMapper() {
    }

    public static Store toDomain(StoreJpaEntity entity) {
        return new Store(entity.getId(), entity.getName(), entity.getDocument(), entity.isActive(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static void apply(Store store, StoreJpaEntity entity) {
        entity.setName(store.name());
        entity.setDocument(store.document());
        entity.setActive(store.active());
    }
}
