package com.empresa.gestao.sale.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleItemRequest(
        @NotNull(message = "Selecione o produto.")
        UUID productId,

        @NotNull(message = "Informe a quantidade.")
        @Min(value = 1, message = "A quantidade deve ser maior que zero.")
        Integer quantity,

        @DecimalMin(value = "0.00", message = "O preço unitário não pode ser negativo.")
        @Digits(integer = 10, fraction = 2, message = "Informe um preço unitário válido.")
        BigDecimal unitPrice
) {
}
