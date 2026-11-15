package com.empresa.gestao.movement.application.dto;

import com.empresa.gestao.shared.domain.model.MovementType;

import java.util.UUID;

public record MovementCommand(
        UUID productId,
        MovementType type,
        Integer quantity,
        String reason,
        String observation
) {
}
