package com.empresa.gestao.store.application.dto;

import java.time.Instant;
import java.util.UUID;

public record StoreResult(
        UUID id,
        String name,
        String document,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
