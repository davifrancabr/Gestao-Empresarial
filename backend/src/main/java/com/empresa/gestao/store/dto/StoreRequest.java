package com.empresa.gestao.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoreRequest(
        @NotBlank(message = "Informe o nome da loja.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String name,

        @Size(max = 20, message = "O documento deve ter no máximo 20 caracteres.")
        String document,

        Boolean active
) {
}
