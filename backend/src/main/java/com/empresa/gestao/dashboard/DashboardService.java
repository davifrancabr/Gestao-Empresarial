package com.empresa.gestao.dashboard;

import com.empresa.gestao.dashboard.dto.DashboardResponse;
import com.empresa.gestao.dashboard.dto.DashboardResponse.TopProductResponse;
import com.empresa.gestao.dashboard.dto.DashboardResponse.TrendPoint;
import com.empresa.gestao.movement.MovementMapper;
import com.empresa.gestao.movement.MovementType;
import com.empresa.gestao.movement.StockMovementRepository;
import com.empresa.gestao.product.ProductRepository;
import com.empresa.gestao.product.ProductService;
import com.empresa.gestao.sale.SaleService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DashboardService {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final StockMovementRepository movementRepository;
    private final MovementMapper movementMapper;
    private final SaleService saleService;

    public DashboardService(ProductRepository productRepository, ProductService productService,
                            StockMovementRepository movementRepository, MovementMapper movementMapper,
                            SaleService saleService) {
        this.productRepository = productRepository;
        this.productService = productService;
        this.movementRepository = movementRepository;
        this.movementMapper = movementMapper;
        this.saleService = saleService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse overview(UUID storeId) {
        long total = productRepository.countByStoreIdAndActiveTrue(storeId);
        long out = productRepository.countByStoreIdAndActiveTrueAndQuantity(storeId, 0);
        long low = productRepository.countLowStock(storeId);
        long normal = Math.max(total - low - out, 0);
        long units = productRepository.sumQuantity(storeId);

        long movements = movementRepository.countByStoreId(storeId);
        long entries = movementRepository.countByStoreIdAndType(storeId, MovementType.ENTRY);
        long exits = movementRepository.countByStoreIdAndType(storeId, MovementType.EXIT);

        List<DashboardResponse.TopProductResponse> top = movementRepository
                .findTopMoved(storeId, PageRequest.of(0, 5)).stream()
                .map(aggregate -> new TopProductResponse(
                        aggregate.getProductId(),
                        aggregate.getProductName(),
                        aggregate.getTotalQuantity() == null ? 0 : aggregate.getTotalQuantity(),
                        aggregate.getMovementCount() == null ? 0 : aggregate.getMovementCount()))
                .toList();

        return new DashboardResponse(
                total,
                normal,
                low,
                out,
                low + out,
                movements,
                entries,
                exits,
                saleService.count(storeId),
                saleService.totalRevenue(storeId),
                units,
                movementRepository.findTop5ByStoreIdOrderByCreatedAtDesc(storeId).stream()
                        .map(movementMapper::toResponse).toList(),
                productService.lowStock(storeId, 5),
                top,
                productService.lowTurnover(storeId, 30, 5),
                trend(storeId, 7)
        );
    }

    @Transactional(readOnly = true)
    public List<TrendPoint> trend(UUID storeId, int days) {
        int safeDays = Math.min(Math.max(days, 1), 90);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = today.minusDays(safeDays - 1L);
        Instant since = start.atStartOfDay(ZoneOffset.UTC).toInstant();

        Map<LocalDate, long[]> byDay = new LinkedHashMap<>();
        for (int i = 0; i < safeDays; i++) {
            byDay.put(start.plusDays(i), new long[]{0, 0});
        }

        for (StockMovementRepository.DailyMovementCount daily : movementRepository.countDaily(storeId, since)) {
            LocalDate day = daily.getDay().toLocalDate();
            long[] values = byDay.get(day);
            if (values == null) {
                continue;
            }
            if ("ENTRY".equals(daily.getType())) {
                values[0] = daily.getTotal() == null ? 0 : daily.getTotal();
            } else if ("EXIT".equals(daily.getType())) {
                values[1] = daily.getTotal() == null ? 0 : daily.getTotal();
            }
        }

        List<TrendPoint> trend = new ArrayList<>();
        byDay.forEach((date, values) -> trend.add(new TrendPoint(date, values[0], values[1])));
        return trend;
    }
}
