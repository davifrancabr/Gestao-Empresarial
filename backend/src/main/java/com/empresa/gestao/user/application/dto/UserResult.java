package com.empresa.gestao.user.application.dto;

import com.empresa.gestao.shared.domain.model.Role;
import com.empresa.gestao.store.application.dto.StoreSummary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResult(
        UUID id,
        String name,
        String email,
        Role role,
        boolean active,
        List<StoreSummary> stores,
        Instant createdAt
) {
}
