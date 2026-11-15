package com.empresa.gestao.product.application.usecase;

import com.empresa.gestao.product.application.dto.StockSummary;
import com.empresa.gestao.product.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetStockSummaryUseCase {

    private final ProductRepository productRepository;

    public GetStockSummaryUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public StockSummary execute(UUID storeId) {
        long total = productRepository.countActive(storeId);
        long out = productRepository.countByQuantity(storeId, 0);
        long low = productRepository.countLowStock(storeId);
        long normal = Math.max(total - low - out, 0);
        long units = productRepository.sumQuantity(storeId);
        return new StockSummary(total, normal, low, out, low + out, units);
    }
}
