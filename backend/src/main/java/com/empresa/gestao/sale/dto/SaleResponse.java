package com.empresa.gestao.sale.dto;

import com.empresa.gestao.sale.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SaleResponse(
        UUID id,
        UUID storeId,
        UUID customerId,
        String customerName,
        UUID userId,
        String userName,
        BigDecimal total,
        SaleStatus status,
        Instant createdAt,
        List<SaleItemResponse> items
) {
}
