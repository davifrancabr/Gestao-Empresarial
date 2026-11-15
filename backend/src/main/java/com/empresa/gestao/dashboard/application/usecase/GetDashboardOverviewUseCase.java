package com.empresa.gestao.dashboard.application.usecase;

import com.empresa.gestao.dashboard.application.dto.DashboardResult;
import com.empresa.gestao.movement.application.dto.MovementStats;
import com.empresa.gestao.movement.application.usecase.GetMovementStatsUseCase;
import com.empresa.gestao.movement.application.usecase.GetMovementTrendUseCase;
import com.empresa.gestao.movement.application.usecase.GetRecentMovementsUseCase;
import com.empresa.gestao.movement.application.usecase.GetTopMovedProductsUseCase;
import com.empresa.gestao.product.application.dto.StockSummary;
import com.empresa.gestao.product.application.usecase.GetStockSummaryUseCase;
import com.empresa.gestao.product.application.usecase.LowStockProductsUseCase;
import com.empresa.gestao.product.application.usecase.LowTurnoverProductsUseCase;
import com.empresa.gestao.sale.application.usecase.CountSalesUseCase;
import com.empresa.gestao.sale.application.usecase.GetRevenueUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Indicadores do painel. Compõe casos de uso de leitura dos módulos de produto,
 * movimentação e venda, sem acessar repositórios de outros módulos.
 */
@Service
public class GetDashboardOverviewUseCase {

    private static final int RECENT_LIMIT = 5;
    private static final int LOW_TURNOVER_DAYS = 30;
    private static final int TREND_DAYS = 7;

    private final GetStockSummaryUseCase getStockSummary;
    private final GetMovementStatsUseCase getMovementStats;
    private final GetTopMovedProductsUseCase getTopMovedProducts;
    private final GetRecentMovementsUseCase getRecentMovements;
    private final GetMovementTrendUseCase getMovementTrend;
    private final LowStockProductsUseCase lowStockProducts;
    private final LowTurnoverProductsUseCase lowTurnoverProducts;
    private final CountSalesUseCase countSales;
    private final GetRevenueUseCase getRevenue;

    public GetDashboardOverviewUseCase(GetStockSummaryUseCase getStockSummary,
                                       GetMovementStatsUseCase getMovementStats,
                                       GetTopMovedProductsUseCase getTopMovedProducts,
                                       GetRecentMovementsUseCase getRecentMovements,
                                       GetMovementTrendUseCase getMovementTrend,
                                       LowStockProductsUseCase lowStockProducts,
                                       LowTurnoverProductsUseCase lowTurnoverProducts,
                                       CountSalesUseCase countSales, GetRevenueUseCase getRevenue) {
        this.getStockSummary = getStockSummary;
        this.getMovementStats = getMovementStats;
        this.getTopMovedProducts = getTopMovedProducts;
        this.getRecentMovements = getRecentMovements;
        this.getMovementTrend = getMovementTrend;
        this.lowStockProducts = lowStockProducts;
        this.lowTurnoverProducts = lowTurnoverProducts;
        this.countSales = countSales;
        this.getRevenue = getRevenue;
    }

    @Transactional(readOnly = true)
    public DashboardResult execute(UUID storeId) {
        StockSummary stock = getStockSummary.execute(storeId);
        MovementStats movements = getMovementStats.execute(storeId);

        return new DashboardResult(
                stock.totalProducts(),
                stock.normal(),
                stock.low(),
                stock.outOfStock(),
                stock.needsRestock(),
                movements.total(),
                movements.entries(),
                movements.exits(),
                countSales.execute(storeId),
                getRevenue.execute(storeId),
                stock.totalUnits(),
                getRecentMovements.execute(storeId, RECENT_LIMIT),
                lowStockProducts.execute(storeId, RECENT_LIMIT),
                getTopMovedProducts.execute(storeId, null, RECENT_LIMIT),
                lowTurnoverProducts.execute(storeId, LOW_TURNOVER_DAYS, RECENT_LIMIT),
                getMovementTrend.execute(storeId, TREND_DAYS)
        );
    }
}
