package com.empresa.gestao.store.application.usecase;

import com.empresa.gestao.store.application.dto.StoreResult;
import com.empresa.gestao.store.domain.model.Store;

final class StoreResultMapper {

    private StoreResultMapper() {
    }

    static StoreResult toResult(Store store) {
        return new StoreResult(store.id(), store.name(), store.document(), store.active(),
                store.createdAt(), store.updatedAt());
    }
}
