package com.empresa.gestao.user.application.usecase;

import com.empresa.gestao.store.application.dto.StoreSummary;
import com.empresa.gestao.store.application.port.StoreQueryPort;
import com.empresa.gestao.user.application.dto.UserInfo;
import com.empresa.gestao.user.application.dto.UserResult;
import com.empresa.gestao.user.domain.model.User;

import java.util.List;

final class UserResultMapper {

    private UserResultMapper() {
    }

    static UserResult toResult(User user, StoreQueryPort storeQueryPort) {
        List<StoreSummary> stores = user.storeIds().stream()
                .map(storeQueryPort::findStoreById)
                .flatMap(java.util.Optional::stream)
                .map(store -> new StoreSummary(store.id(), store.name()))
                .toList();
        return new UserResult(user.id(), user.name(), user.email(), user.role(), user.active(),
                stores, user.createdAt());
    }

    static UserInfo toInfo(User user) {
        return new UserInfo(user.id(), user.name(), user.email(), user.role(), user.active());
    }
}
