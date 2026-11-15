package com.empresa.gestao.customer.application.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomerResult(
        UUID id,
        UUID storeId,
        String name,
        String cpf,
        String phone,
        String email,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
