package com.empresa.gestao.sale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record SaleCreateRequest(
        UUID customerId,

        @NotEmpty(message = "Adicione ao menos um produto à venda.")
        List<@Valid SaleItemRequest> items
) {
}
