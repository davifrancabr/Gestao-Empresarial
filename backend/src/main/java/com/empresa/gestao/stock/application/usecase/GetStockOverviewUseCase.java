package com.empresa.gestao.stock.application.usecase;

import com.empresa.gestao.product.application.dto.ProductResult;
import com.empresa.gestao.product.application.dto.StockSummary;
import com.empresa.gestao.product.application.usecase.GetStockSummaryUseCase;
import com.empresa.gestao.product.application.usecase.ListProductsUseCase;
import com.empresa.gestao.shared.domain.query.PageResult;
import com.empresa.gestao.shared.domain.model.ProductStatus;
import com.empresa.gestao.stock.application.dto.StockOverviewResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetStockOverviewUseCase {

    private final GetStockSummaryUseCase getStockSummary;
    private final ListProductsUseCase listProducts;

    public GetStockOverviewUseCase(GetStockSummaryUseCase getStockSummary,
                                   ListProductsUseCase listProducts) {
        this.getStockSummary = getStockSummary;
        this.listProducts = listProducts;
    }

    @Transactional(readOnly = true)
    public StockOverviewResult execute(UUID storeId, String search, String category,
                                       ProductStatus status, String sort, Integer page,
                                       Integer size) {
        StockSummary summary = getStockSummary.execute(storeId);
        PageResult<ProductResult> products = listProducts.execute(storeId, search, category, status,
                sort, page, size, false);
        return StockOverviewResult.of(summary, products);
    }
}
