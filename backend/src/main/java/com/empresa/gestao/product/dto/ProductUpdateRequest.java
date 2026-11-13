package com.empresa.gestao.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductUpdateRequest(
        @NotBlank(message = "Informe o nome do produto.")
        @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres.")
        String name,

        @Size(max = 40, message = "O SKU deve ter no máximo 40 caracteres.")
        String sku,

        @Size(max = 60, message = "A categoria deve ter no máximo 60 caracteres.")
        String category,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String description,

        @NotNull(message = "Informe o estoque mínimo.")
        @Min(value = 0, message = "O estoque mínimo não pode ser negativo.")
        Integer minimum,

        @Size(max = 10, message = "A unidade deve ter no máximo 10 caracteres.")
        String unit,

        Long version
) {
}
