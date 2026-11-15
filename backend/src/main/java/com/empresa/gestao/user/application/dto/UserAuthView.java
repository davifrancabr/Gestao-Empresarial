package com.empresa.gestao.user.application.dto;

import com.empresa.gestao.shared.domain.model.Role;
import com.empresa.gestao.store.application.dto.StoreSummary;

import java.util.List;
import java.util.UUID;

public record UserAuthView(
        UUID id,
        String name,
        String email,
        String passwordHash,
        Role role,
        boolean active,
        List<StoreSummary> stores
) {
}
