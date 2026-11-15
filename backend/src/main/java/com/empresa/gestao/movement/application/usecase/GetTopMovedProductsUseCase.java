package com.empresa.gestao.movement.application.usecase;

import com.empresa.gestao.movement.application.dto.TopProductResult;
import com.empresa.gestao.movement.domain.repository.StockMovementRepository;
import com.empresa.gestao.movement.domain.repository.TopMovedProduct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class GetTopMovedProductsUseCase {

    private final StockMovementRepository movementRepository;

    public GetTopMovedProductsUseCase(StockMovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    @Transactional(readOnly = true)
    public List<TopProductResult> execute(UUID storeId, Integer days, Integer limit) {
        int safeLimit = Math.min(Math.max(limit == null ? 10 : limit, 1), 100);
        List<TopMovedProduct> products;
        if (days == null) {
            products = movementRepository.findTopMoved(storeId, safeLimit);
        } else {
            int safeDays = Math.min(Math.max(days, 1), 365);
            Instant since = Instant.now().minus(safeDays, ChronoUnit.DAYS);
            products = movementRepository.findTopMovedSince(storeId, since, safeLimit);
        }
        return products.stream().map(GetTopMovedProductsUseCase::toResult).toList();
    }

    private static TopProductResult toResult(TopMovedProduct product) {
        return new TopProductResult(product.productId(), product.name(),
                product.totalQuantity(), product.movementCount());
    }
}
