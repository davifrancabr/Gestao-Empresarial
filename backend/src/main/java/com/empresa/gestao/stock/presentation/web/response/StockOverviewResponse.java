package com.empresa.gestao.stock.presentation.web.response;

import com.empresa.gestao.product.application.dto.ProductResult;
import com.empresa.gestao.shared.presentation.web.PageResponse;
import com.empresa.gestao.stock.application.dto.StockOverviewResult;

public record StockOverviewResponse(
        long totalProducts,
        long normal,
        long low,
        long outOfStock,
        long needsRestock,
        long totalUnits,
        PageResponse<ProductResult> products
) {

    public static StockOverviewResponse from(StockOverviewResult result) {
        return new StockOverviewResponse(
                result.totalProducts(),
                result.normal(),
                result.low(),
                result.outOfStock(),
                result.needsRestock(),
                result.totalUnits(),
                PageResponse.from(result.products())
        );
    }
}
