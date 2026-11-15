package com.empresa.gestao.shared.application.dto;

import com.empresa.gestao.shared.domain.model.Role;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String name,
        String email,
        Role role,
        boolean admin,
        boolean canManage,
        List<UserStoreRef> stores
) {

    public List<UUID> storeIds() {
        return stores.stream().map(UserStoreRef::id).toList();
    }

    public boolean hasStoreAccess(UUID storeId) {
        return admin || storeIds().contains(storeId);
    }
}
