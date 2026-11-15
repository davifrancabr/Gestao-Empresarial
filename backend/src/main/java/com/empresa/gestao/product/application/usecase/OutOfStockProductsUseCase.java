package com.empresa.gestao.product.application.usecase;

import com.empresa.gestao.product.application.dto.ProductResult;
import com.empresa.gestao.product.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OutOfStockProductsUseCase {

    private final ProductRepository productRepository;

    public OutOfStockProductsUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResult> execute(UUID storeId, int limit) {
        return productRepository.findOutOfStock(storeId, limit).stream()
                .map(ProductResultMapper::toResult)
                .toList();
    }
}
