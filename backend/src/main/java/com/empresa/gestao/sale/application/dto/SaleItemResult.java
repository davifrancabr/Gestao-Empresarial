package com.empresa.gestao.sale.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleItemResult(
        UUID id,
        UUID productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
