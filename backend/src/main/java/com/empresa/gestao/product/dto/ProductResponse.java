package com.empresa.gestao.product.dto;

import com.empresa.gestao.product.ProductStatus;

import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
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
