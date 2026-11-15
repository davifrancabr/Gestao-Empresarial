package com.empresa.gestao.movement.domain.repository;

import com.empresa.gestao.shared.domain.model.MovementType;

import java.time.Instant;
import java.util.UUID;

public record MovementFilter(
        UUID storeId,
        UUID productId,
        MovementType type,
        Instant from,
        Instant to,
        String searchTerm
) {
}
