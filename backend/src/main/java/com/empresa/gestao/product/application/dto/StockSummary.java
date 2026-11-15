package com.empresa.gestao.product.application.dto;

public record StockSummary(
        long totalProducts,
        long normal,
        long low,
        long outOfStock,
        long needsRestock,
        long totalUnits
) {
}
