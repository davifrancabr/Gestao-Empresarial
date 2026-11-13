package com.empresa.gestao.sale.dto;

import com.empresa.gestao.sale.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaleSummaryResponse(
        UUID id,
        UUID customerId,
        String customerName,
        String userName,
        int itemCount,
        BigDecimal total,
        SaleStatus status,
        Instant createdAt
) {
}
