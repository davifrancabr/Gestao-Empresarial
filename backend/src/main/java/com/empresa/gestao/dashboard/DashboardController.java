package com.empresa.gestao.dashboard;

import com.empresa.gestao.dashboard.dto.DashboardResponse;
import com.empresa.gestao.product.ProductService;
import com.empresa.gestao.product.dto.ProductResponse;
import com.empresa.gestao.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final ProductService productService;
    private final CurrentUserService currentUser;

    public DashboardController(DashboardService dashboardService, ProductService productService,
                               CurrentUserService currentUser) {
        this.dashboardService = dashboardService;
        this.productService = productService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public DashboardResponse overview(@RequestParam(required = false) UUID storeId) {
        return dashboardService.overview(currentUser.resolveStoreId(storeId));
    }

    @GetMapping("/movements")
    public List<DashboardResponse.TrendPoint> trend(@RequestParam(required = false) UUID storeId,
                                                    @RequestParam(defaultValue = "7") int days) {
        return dashboardService.trend(currentUser.resolveStoreId(storeId), days);
    }

    @GetMapping("/low-stock")
    public List<ProductResponse> lowStock(@RequestParam(required = false) UUID storeId) {
        return productService.lowStock(currentUser.resolveStoreId(storeId), 20);
    }

    @GetMapping("/out-of-stock")
    public List<ProductResponse> outOfStock(@RequestParam(required = false) UUID storeId) {
        return productService.outOfStock(currentUser.resolveStoreId(storeId), 20);
    }
}
