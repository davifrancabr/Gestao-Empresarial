package com.empresa.gestao.stock;

import com.empresa.gestao.product.ProductRepository;
import com.empresa.gestao.product.ProductService;
import com.empresa.gestao.product.ProductStatus;
import com.empresa.gestao.stock.dto.StockOverviewResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class StockService {

    private final ProductRepository productRepository;
    private final ProductService productService;

    public StockService(ProductRepository productRepository, ProductService productService) {
        this.productRepository = productRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public StockOverviewResponse overview(UUID storeId, String search, String category, ProductStatus status,
                                          Integer page, Integer size, String sort) {
        long total = productRepository.countByStoreIdAndActiveTrue(storeId);
        long out = productRepository.countByStoreIdAndActiveTrueAndQuantity(storeId, 0);
        long low = productRepository.countLowStock(storeId);
        long normal = Math.max(total - low - out, 0);
        long units = productRepository.sumQuantity(storeId);

        return new StockOverviewResponse(
                total,
                normal,
                low,
                out,
                low + out,
                units,
                productService.list(storeId, search, category, status, sort, page, size, false)
        );
    }
}
