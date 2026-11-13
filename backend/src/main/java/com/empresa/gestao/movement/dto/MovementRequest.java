package com.empresa.gestao.movement.dto;

import com.empresa.gestao.movement.MovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MovementRequest(
        @NotNull(message = "Selecione o produto.")
        UUID productId,

        @NotNull(message = "Selecione o tipo de movimentação.")
        MovementType type,

        @NotNull(message = "Informe a quantidade.")
        @PositiveOrZero(message = "A quantidade não pode ser negativa.")
        @Min(value = 0, message = "A quantidade não pode ser negativa.")
        Integer quantity,

        @NotBlank(message = "Informe o motivo da movimentação.")
        @Size(max = 120, message = "O motivo deve ter no máximo 120 caracteres.")
        String reason,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
        String observation
) {
}
