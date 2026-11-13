package com.empresa.gestao.user.dto;

import com.empresa.gestao.store.dto.StoreSummary;
import com.empresa.gestao.user.Role;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        boolean active,
        List<StoreSummary> stores,
        Instant createdAt
) {
}
