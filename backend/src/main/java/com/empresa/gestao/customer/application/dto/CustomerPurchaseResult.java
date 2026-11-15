package com.empresa.gestao.customer.application.dto;

import com.empresa.gestao.shared.domain.model.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerPurchaseResult(
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
