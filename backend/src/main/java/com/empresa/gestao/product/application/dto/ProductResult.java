package com.empresa.gestao.product.application.dto;

import com.empresa.gestao.shared.domain.model.ProductStatus;

import java.time.Instant;
import java.util.UUID;

public record ProductResult(
        UUID id,
        UUID storeId,
        String name,
        String sku,
        String category,
        String description,
        int quantity,
        int minimum,
        String unit,
        ProductStatus status,
        boolean active,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
}
