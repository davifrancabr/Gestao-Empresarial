package com.empresa.gestao.dashboard.application.dto;

import com.empresa.gestao.movement.application.dto.MovementResult;
import com.empresa.gestao.movement.application.dto.TopProductResult;
import com.empresa.gestao.movement.application.dto.TrendPoint;
import com.empresa.gestao.product.application.dto.ProductResult;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResult(
        long productsTotal,
        long normal,
        long low,
        long outOfStock,
        long needsRestock,
        long movementsTotal,
        long entries,
        long exits,
        long salesCount,
        BigDecimal salesRevenue,
        long totalUnits,
        List<MovementResult> recentMovements,
        List<ProductResult> lowStockProducts,
        List<TopProductResult> topMovedProducts,
        List<ProductResult> lowTurnoverProducts,
        List<TrendPoint> trend
) {
}
