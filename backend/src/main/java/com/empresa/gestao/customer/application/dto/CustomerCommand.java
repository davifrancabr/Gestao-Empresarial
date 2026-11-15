package com.empresa.gestao.customer.application.dto;

public record CustomerCommand(
        String name,
        String cpf,
        String phone,
        String email,
        Boolean active
) {
}
