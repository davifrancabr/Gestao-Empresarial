package com.empresa.gestao.product.domain.repository;

import com.empresa.gestao.shared.domain.model.ProductStatus;

import java.util.UUID;

public record ProductFilter(
        UUID storeId,
        String search,
        String category,
        ProductStatus status,
        boolean includeInactive
) {
}
