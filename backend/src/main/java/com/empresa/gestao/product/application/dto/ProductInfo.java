package com.empresa.gestao.product.application.dto;

import java.util.UUID;

public record ProductInfo(
        UUID id,
        UUID storeId,
        String name,
        String sku,
        int quantity,
        int minimum,
        String unit,
        boolean active,
        Long version
) {
}
