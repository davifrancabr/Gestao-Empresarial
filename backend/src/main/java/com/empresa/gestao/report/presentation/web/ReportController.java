package com.empresa.gestao.report.presentation.web;

import com.empresa.gestao.movement.application.dto.MovementResult;
import com.empresa.gestao.movement.application.dto.TopProductResult;
import com.empresa.gestao.movement.application.usecase.GetTopMovedProductsUseCase;
import com.empresa.gestao.movement.application.usecase.SearchMovementsUseCase;
import com.empresa.gestao.product.application.dto.ProductResult;
import com.empresa.gestao.product.application.usecase.ListProductsUseCase;
import com.empresa.gestao.product.application.usecase.LowTurnoverProductsUseCase;
import com.empresa.gestao.report.application.usecase.GetSalesReportUseCase;
import com.empresa.gestao.report.presentation.web.response.SalesReportResponse;
import com.empresa.gestao.report.presentation.web.response.StockReportResponse;
import com.empresa.gestao.shared.domain.model.MovementType;
import com.empresa.gestao.shared.domain.model.ProductStatus;
import com.empresa.gestao.shared.presentation.web.PageResponse;
import com.empresa.gestao.stock.application.usecase.GetStockOverviewUseCase;
import com.empresa.gestao.store.application.usecase.ResolveStoreAccessUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final GetSalesReportUseCase getSalesReport;
    private final GetStockOverviewUseCase getStockOverview;
    private final ListProductsUseCase listProducts;
    private final SearchMovementsUseCase searchMovements;
    private final GetTopMovedProductsUseCase getTopMovedProducts;
    private final LowTurnoverProductsUseCase lowTurnoverProducts;
    private final ResolveStoreAccessUseCase resolveStore;

    public ReportController(GetSalesReportUseCase getSalesReport,
                            GetStockOverviewUseCase getStockOverview,
                            ListProductsUseCase listProducts,
                            SearchMovementsUseCase searchMovements,
                            GetTopMovedProductsUseCase getTopMovedProducts,
                            LowTurnoverProductsUseCase lowTurnoverProducts,
                            ResolveStoreAccessUseCase resolveStore) {
        this.getSalesReport = getSalesReport;
        this.getStockOverview = getStockOverview;
        this.listProducts = listProducts;
        this.searchMovements = searchMovements;
        this.getTopMovedProducts = getTopMovedProducts;
        this.lowTurnoverProducts = lowTurnoverProducts;
        this.resolveStore = resolveStore;
    }

    @GetMapping("/products")
    public PageResponse<ProductResult> products(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        UUID store = resolveStore.execute(storeId);
        return PageResponse.from(listProducts.execute(store, search, category, status, sort,
                page, size, includeInactive));
    }

    @GetMapping("/stock")
    public StockReportResponse stock(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        UUID store = resolveStore.execute(storeId);
        return StockReportResponse.from(
                getStockOverview.execute(store, search, category, status, sort, page, size));
    }

    @GetMapping("/movements")
    public PageResponse<MovementResult> movements(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        UUID store = resolveStore.execute(storeId);
        return PageResponse.from(searchMovements.execute(store, productId, type, from, to, search,
                page, size, sort));
    }

    @GetMapping("/sales")
    public SalesReportResponse sales(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        UUID store = resolveStore.execute(storeId);
        return SalesReportResponse.from(getSalesReport.execute(store, customerId, from, to,
                search, page, size, sort));
    }

    @GetMapping("/top-products")
    public List<TopProductResult> topProducts(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit) {
        UUID store = resolveStore.execute(storeId);
        return getTopMovedProducts.execute(store, days, limit);
    }

    @GetMapping("/low-turnover")
    public List<ProductResult> lowTurnover(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit) {
        UUID store = resolveStore.execute(storeId);
        return lowTurnoverProducts.execute(store, days, limit);
    }
}
