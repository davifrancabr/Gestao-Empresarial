package com.empresa.gestao.product.application.usecase;

import com.empresa.gestao.product.application.dto.ProductResult;
import com.empresa.gestao.product.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class LowTurnoverProductsUseCase {

    private final ProductRepository productRepository;

    public LowTurnoverProductsUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResult> execute(UUID storeId, int days, int limit) {
        int safeDays = Math.min(Math.max(days, 1), 365);
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        Instant since = Instant.now().minus(safeDays, ChronoUnit.DAYS);
        return productRepository.findLowTurnover(storeId, since, safeLimit).stream()
                .map(ProductResultMapper::toResult)
                .toList();
    }
}
