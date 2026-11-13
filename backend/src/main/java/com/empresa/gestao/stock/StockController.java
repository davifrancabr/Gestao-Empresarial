package com.empresa.gestao.stock;

import com.empresa.gestao.product.ProductStatus;
import com.empresa.gestao.security.CurrentUserService;
import com.empresa.gestao.stock.dto.StockOverviewResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock")
public class StockController {

    private final StockService stockService;
    private final CurrentUserService currentUser;

    public StockController(StockService stockService, CurrentUserService currentUser) {
        this.stockService = stockService;
        this.currentUser = currentUser;
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
        UUID store = currentUser.resolveStoreId(storeId);
        return stockService.overview(store, search, category, status, page, size, sort);
    }
}
