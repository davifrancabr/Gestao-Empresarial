package com.empresa.gestao.stock.presentation.web;

import com.empresa.gestao.shared.domain.model.ProductStatus;
import com.empresa.gestao.stock.application.usecase.GetStockOverviewUseCase;
import com.empresa.gestao.stock.presentation.web.response.StockOverviewResponse;
import com.empresa.gestao.store.application.usecase.ResolveStoreAccessUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock")
public class StockController {

    private final GetStockOverviewUseCase getStockOverview;
    private final ResolveStoreAccessUseCase resolveStore;

    public StockController(GetStockOverviewUseCase getStockOverview,
                           ResolveStoreAccessUseCase resolveStore) {
        this.getStockOverview = getStockOverview;
        this.resolveStore = resolveStore;
    }

    @GetMapping
    public StockOverviewResponse overview(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        UUID store = resolveStore.execute(storeId);
        return StockOverviewResponse.from(
                getStockOverview.execute(store, search, category, status, sort, page, size));
    }
}
