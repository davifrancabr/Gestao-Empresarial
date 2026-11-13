package com.empresa.gestao.dashboard.dto;

import com.empresa.gestao.movement.dto.MovementResponse;
import com.empresa.gestao.product.dto.ProductResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DashboardResponse(
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
        List<MovementResponse> recentMovements,
        List<ProductResponse> lowStockProducts,
        List<TopProductResponse> topMovedProducts,
        List<ProductResponse> lowTurnoverProducts,
        List<TrendPoint> trend
) {

    public record TopProductResponse(
            UUID productId,
            String name,
            long totalQuantity,
            long movementCount
    ) {
    }

    public record TrendPoint(
            LocalDate date,
            long entries,
            long exits
    ) {
    }
}
