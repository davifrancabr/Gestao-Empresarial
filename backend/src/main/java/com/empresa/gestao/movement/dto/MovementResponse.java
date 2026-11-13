package com.empresa.gestao.movement.dto;

import com.empresa.gestao.movement.MovementType;

import java.time.Instant;
import java.util.UUID;

public record MovementResponse(
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
