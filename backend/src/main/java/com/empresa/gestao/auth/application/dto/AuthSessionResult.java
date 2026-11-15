package com.empresa.gestao.auth.application.dto;

import com.empresa.gestao.shared.domain.model.Role;
import com.empresa.gestao.store.application.dto.StoreSummary;

import java.util.List;
import java.util.UUID;

public record AuthSessionResult(
        UserSummary user,
        List<StoreSummary> stores,
        UUID activeStoreId
) {

    public record UserSummary(UUID id, String name, String email, Role role) {
    }
}
