package com.empresa.gestao.movement.application.dto;

import com.empresa.gestao.shared.domain.model.MovementType;

import java.time.Instant;
import java.util.UUID;

public record MovementResult(
        UUID id,
        UUID storeId,
        UUID productId,
        String productName,
        String productSku,
        MovementType type,
        int quantity,
        int previousQuantity,
        int resultingQuantity,
        String reason,
        String observation,
        String userName,
        Instant createdAt
) {
}
