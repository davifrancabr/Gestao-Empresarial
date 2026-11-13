package com.empresa.gestao.auth.dto;

import com.empresa.gestao.store.dto.StoreSummary;
import com.empresa.gestao.user.Role;

import java.util.List;
import java.util.UUID;

public record AuthSessionResponse(
        UserSummary user,
        List<StoreSummary> stores,
        UUID activeStoreId
) {

    public record UserSummary(UUID id, String name, String email, Role role) {
    }
}
