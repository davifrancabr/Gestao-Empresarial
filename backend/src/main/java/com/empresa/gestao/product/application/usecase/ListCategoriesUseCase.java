package com.empresa.gestao.product.application.usecase;

import com.empresa.gestao.product.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ListCategoriesUseCase {

    private final ProductRepository productRepository;

    public ListCategoriesUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<String> execute(UUID storeId) {
        return productRepository.findCategories(storeId);
    }
}
