package com.empresa.gestao.report;

import com.empresa.gestao.dashboard.dto.DashboardResponse.TopProductResponse;
import com.empresa.gestao.common.dto.PageResponse;
import com.empresa.gestao.movement.MovementService;
import com.empresa.gestao.movement.MovementType;
import com.empresa.gestao.movement.StockMovementRepository;
import com.empresa.gestao.movement.dto.MovementResponse;
import com.empresa.gestao.product.ProductService;
import com.empresa.gestao.product.ProductStatus;
import com.empresa.gestao.product.dto.ProductResponse;
import com.empresa.gestao.report.dto.SalesReportResponse;
import com.empresa.gestao.sale.SaleService;
import com.empresa.gestao.sale.dto.SaleSummaryResponse;
import com.empresa.gestao.stock.StockService;
import com.empresa.gestao.stock.dto.StockOverviewResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    private final ProductService productService;
    private final StockService stockService;
    private final MovementService movementService;
    private final SaleService saleService;
    private final StockMovementRepository movementRepository;

    public ReportService(ProductService productService, StockService stockService,
                         MovementService movementService, SaleService saleService,
                         StockMovementRepository movementRepository) {
        this.productService = productService;
        this.stockService = stockService;
        this.movementService = movementService;
        this.saleService = saleService;
        this.movementRepository = movementRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> products(UUID storeId, String search, String category,
                                                  ProductStatus status, String sort, Integer page,
                                                  Integer size, boolean includeInactive) {
        return productService.list(storeId, search, category, status, sort, page, size, includeInactive);
    }

    @Transactional(readOnly = true)
    public StockOverviewResponse stock(UUID storeId, String search, String category, ProductStatus status,
                                       Integer page, Integer size, String sort) {
        return stockService.overview(storeId, search, category, status, page, size, sort);
    }

    @Transactional(readOnly = true)
    public PageResponse<MovementResponse> movements(UUID storeId, UUID productId, MovementType type,
                                                    LocalDate from, LocalDate to, String search,
                                                    Integer page, Integer size, String sort) {
        return movementService.search(storeId, productId, type, from, to, search, page, size, sort);
    }

    @Transactional(readOnly = true)
    public SalesReportResponse sales(UUID storeId, UUID customerId, LocalDate from, LocalDate to,
                                     String search, Integer page, Integer size, String sort) {
        PageResponse<SaleSummaryResponse> sales = saleService.list(storeId, customerId, from, to,
                search, page, size, sort);
        return new SalesReportResponse(
                sales,
                saleService.totalRevenue(storeId, from, to),
                sales.totalElements()
        );
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> topProducts(UUID storeId, int days, int limit) {
        int safeDays = Math.min(Math.max(days, 1), 365);
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        Instant since = Instant.now().minus(safeDays, ChronoUnit.DAYS);
        return movementRepository.findTopMovedSince(storeId, since, PageRequest.of(0, safeLimit)).stream()
                .map(aggregate -> new TopProductResponse(
                        aggregate.getProductId(),
                        aggregate.getProductName(),
                        aggregate.getTotalQuantity() == null ? 0 : aggregate.getTotalQuantity(),
                        aggregate.getMovementCount() == null ? 0 : aggregate.getMovementCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> lowTurnover(UUID storeId, int days, int limit) {
        return productService.lowTurnover(storeId, Math.min(Math.max(days, 1), 365),
                Math.min(Math.max(limit, 1), 100));
    }
}
