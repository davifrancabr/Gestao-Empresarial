package com.empresa.gestao.report;

import com.empresa.gestao.common.dto.PageResponse;
import com.empresa.gestao.dashboard.dto.DashboardResponse.TopProductResponse;
import com.empresa.gestao.movement.MovementType;
import com.empresa.gestao.movement.dto.MovementResponse;
import com.empresa.gestao.product.ProductStatus;
import com.empresa.gestao.product.dto.ProductResponse;
import com.empresa.gestao.report.dto.SalesReportResponse;
import com.empresa.gestao.security.CurrentUserService;
import com.empresa.gestao.stock.dto.StockOverviewResponse;
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

    private final ReportService reportService;
    private final CurrentUserService currentUser;

    public ReportController(ReportService reportService, CurrentUserService currentUser) {
        this.reportService = reportService;
        this.currentUser = currentUser;
    }

    @GetMapping("/products")
    public PageResponse<ProductResponse> products(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return reportService.products(currentUser.resolveStoreId(storeId), search, category, status,
                sort, page, size, includeInactive);
    }

    @GetMapping("/stock")
    public StockOverviewResponse stock(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return reportService.stock(currentUser.resolveStoreId(storeId), search, category, status,
                page, size, sort);
    }

    @GetMapping("/movements")
    public PageResponse<MovementResponse> movements(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return reportService.movements(currentUser.resolveStoreId(storeId), productId, type, from, to,
                search, page, size, sort);
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
        return reportService.sales(currentUser.resolveStoreId(storeId), customerId, from, to,
                search, page, size, sort);
    }

    @GetMapping("/top-products")
    public List<TopProductResponse> topProducts(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit) {
        return reportService.topProducts(currentUser.resolveStoreId(storeId), days, limit);
    }

    @GetMapping("/low-turnover")
    public List<ProductResponse> lowTurnover(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit) {
        return reportService.lowTurnover(currentUser.resolveStoreId(storeId), days, limit);
    }
}
