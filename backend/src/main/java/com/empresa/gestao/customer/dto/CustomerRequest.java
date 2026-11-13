package com.empresa.gestao.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "Informe o nome do cliente.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String name,

        @Size(max = 14, message = "O CPF deve ter no máximo 14 caracteres.")
        String cpf,

        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
        String phone,

        @Email(message = "Informe um e-mail válido.")
        @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres.")
        String email,

        Boolean active
) {
}
