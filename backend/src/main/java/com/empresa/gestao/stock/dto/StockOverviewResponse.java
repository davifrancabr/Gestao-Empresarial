package com.empresa.gestao.stock.dto;

import com.empresa.gestao.common.dto.PageResponse;
import com.empresa.gestao.product.dto.ProductResponse;

public record StockOverviewResponse(
        long totalProducts,
        long normal,
        long low,
        long outOfStock,
        long needsRestock,
        long totalUnits,
        PageResponse<ProductResponse> products
) {
}
