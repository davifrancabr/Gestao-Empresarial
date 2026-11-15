package com.empresa.gestao.dashboard.presentation.web;

import com.empresa.gestao.dashboard.application.dto.DashboardResult;
import com.empresa.gestao.dashboard.application.usecase.GetDashboardOverviewUseCase;
import com.empresa.gestao.dashboard.application.usecase.GetDashboardTrendUseCase;
import com.empresa.gestao.movement.application.dto.TrendPoint;
import com.empresa.gestao.product.application.dto.ProductResult;
import com.empresa.gestao.product.application.usecase.LowStockProductsUseCase;
import com.empresa.gestao.product.application.usecase.OutOfStockProductsUseCase;
import com.empresa.gestao.store.application.usecase.ResolveStoreAccessUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private static final int SHORT_LIST_LIMIT = 20;

    private final GetDashboardOverviewUseCase getDashboardOverview;
    private final GetDashboardTrendUseCase getDashboardTrend;
    private final LowStockProductsUseCase lowStockProducts;
    private final OutOfStockProductsUseCase outOfStockProducts;
    private final ResolveStoreAccessUseCase resolveStore;

    public DashboardController(GetDashboardOverviewUseCase getDashboardOverview,
                               GetDashboardTrendUseCase getDashboardTrend,
                               LowStockProductsUseCase lowStockProducts,
                               OutOfStockProductsUseCase outOfStockProducts,
                               ResolveStoreAccessUseCase resolveStore) {
        this.getDashboardOverview = getDashboardOverview;
        this.getDashboardTrend = getDashboardTrend;
        this.lowStockProducts = lowStockProducts;
        this.outOfStockProducts = outOfStockProducts;
        this.resolveStore = resolveStore;
    }

    @GetMapping
    public DashboardResult overview(@RequestParam(required = false) UUID storeId) {
        return getDashboardOverview.execute(resolveStore.execute(storeId));
    }

    @GetMapping("/movements")
    public List<TrendPoint> trend(@RequestParam(required = false) UUID storeId,
                                  @RequestParam(defaultValue = "7") int days) {
        return getDashboardTrend.execute(resolveStore.execute(storeId), days);
    }

    @GetMapping("/low-stock")
    public List<ProductResult> lowStock(@RequestParam(required = false) UUID storeId) {
        return lowStockProducts.execute(resolveStore.execute(storeId), SHORT_LIST_LIMIT);
    }

    @GetMapping("/out-of-stock")
    public List<ProductResult> outOfStock(@RequestParam(required = false) UUID storeId) {
        return outOfStockProducts.execute(resolveStore.execute(storeId), SHORT_LIST_LIMIT);
    }
}
