package com.empresa.gestao.product.application.usecase;

import com.empresa.gestao.product.domain.model.Product;
import com.empresa.gestao.product.domain.repository.ProductRepository;
import com.empresa.gestao.shared.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ArchiveProductUseCase {

    private final ProductRepository productRepository;

    public ArchiveProductUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public void execute(UUID storeId, UUID id) {
        Product product = productRepository.findByIdAndStoreIdAndActiveTrue(id, storeId)
                .orElseThrow(() -> new NotFoundException("Produto não encontrado ou arquivado."));
        productRepository.save(product.archived());
    }
}
