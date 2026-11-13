package com.empresa.gestao.store.dto;

import java.time.Instant;
import java.util.UUID;

public record StoreResponse(
        UUID id,
        String name,
        String document,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
