package com.empresa.gestao.movement.application.dto;

import java.util.UUID;

public record TopProductResult(
        UUID productId,
        String name,
        long totalQuantity,
        long movementCount
) {
}
