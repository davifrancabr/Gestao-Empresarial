package com.empresa.gestao.report.presentation.web.response;

import com.empresa.gestao.product.application.dto.ProductResult;
import com.empresa.gestao.shared.presentation.web.PageResponse;
import com.empresa.gestao.stock.application.dto.StockOverviewResult;

public record StockReportResponse(
        long totalProducts,
        long normal,
        long low,
        long outOfStock,
        long needsRestock,
        long totalUnits,
        PageResponse<ProductResult> products
) {

    public static StockReportResponse from(StockOverviewResult result) {
        return new StockReportResponse(
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
