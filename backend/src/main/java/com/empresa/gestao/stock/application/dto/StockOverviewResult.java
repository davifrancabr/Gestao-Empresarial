package com.empresa.gestao.stock.application.dto;

import com.empresa.gestao.product.application.dto.ProductResult;
import com.empresa.gestao.product.application.dto.StockSummary;
import com.empresa.gestao.shared.domain.query.PageResult;

/**
 * Visão agregada de estoque: indicadores do módulo de produto combinados com a
 * listagem paginada. A conversão para o JSON da API acontece na apresentação.
 */
public record StockOverviewResult(
        long totalProducts,
        long normal,
        long low,
        long outOfStock,
        long needsRestock,
        long totalUnits,
        PageResult<ProductResult> products
) {

    public static StockOverviewResult of(StockSummary summary, PageResult<ProductResult> products) {
        return new StockOverviewResult(
                summary.totalProducts(),
                summary.normal(),
                summary.low(),
                summary.outOfStock(),
                summary.needsRestock(),
                summary.totalUnits(),
                products
        );
    }
}
