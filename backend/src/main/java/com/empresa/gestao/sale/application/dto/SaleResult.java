package com.empresa.gestao.sale.application.dto;

import com.empresa.gestao.shared.domain.model.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SaleResult(
        UUID id,
        UUID storeId,
        UUID customerId,
        String customerName,
        UUID userId,
        String userName,
        BigDecimal total,
        SaleStatus status,
        Instant createdAt,
        List<SaleItemResult> items
) {
}
