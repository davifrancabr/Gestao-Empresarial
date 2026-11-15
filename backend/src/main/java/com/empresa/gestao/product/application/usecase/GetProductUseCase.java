package com.empresa.gestao.product.application.usecase;

import com.empresa.gestao.product.application.dto.ProductResult;
import com.empresa.gestao.product.domain.model.Product;
import com.empresa.gestao.product.domain.repository.ProductRepository;
import com.empresa.gestao.shared.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetProductUseCase {

    private final ProductRepository productRepository;

    public GetProductUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public ProductResult execute(UUID storeId, UUID id) {
        Product product = productRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new NotFoundException("Produto não encontrado."));
        return ProductResultMapper.toResult(product);
    }
}
