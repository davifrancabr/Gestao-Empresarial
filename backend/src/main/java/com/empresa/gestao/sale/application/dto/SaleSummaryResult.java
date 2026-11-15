package com.empresa.gestao.sale.application.dto;

import com.empresa.gestao.shared.domain.model.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SaleSummaryResult(
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
