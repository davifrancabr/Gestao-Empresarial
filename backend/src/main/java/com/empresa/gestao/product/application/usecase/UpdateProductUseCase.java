package com.empresa.gestao.product.application.usecase;

import com.empresa.gestao.product.application.dto.ProductResult;
import com.empresa.gestao.product.application.dto.ProductUpdateCommand;
import com.empresa.gestao.product.domain.model.Product;
import com.empresa.gestao.product.domain.repository.ProductRepository;
import com.empresa.gestao.shared.domain.BusinessRuleException;
import com.empresa.gestao.shared.domain.ConflictException;
import com.empresa.gestao.shared.domain.NotFoundException;
import com.empresa.gestao.shared.domain.util.TextNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class UpdateProductUseCase {

    private final ProductRepository productRepository;

    public UpdateProductUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResult execute(UUID storeId, UUID id, ProductUpdateCommand command) {
        Product product = productRepository.findByIdAndStoreIdAndActiveTrue(id, storeId)
                .orElseThrow(() -> new NotFoundException("Produto não encontrado ou arquivado."));

        if (command.version() != null && product.version() != null
                && !Objects.equals(command.version(), product.version())) {
            throw new ConflictException("O produto foi alterado por outra operação. Recarregue os dados.");
        }

        String name = cleanName(command.name());
        String normalizedName = TextNormalizer.normalizeName(name);
        String sku = cleanSku(command.sku());
        ensureUnique(storeId, normalizedName, sku, id);

        Product updated = product.withDetails(name, normalizedName, sku,
                cleanCategory(command.category()), TextNormalizer.cleanText(command.description()),
                command.minimum(), cleanUnit(command.unit()));

        // Flush para que a resposta carregue a versão otimista já incrementada.
        return ProductResultMapper.toResult(productRepository.saveAndFlush(updated));
    }

    private void ensureUnique(UUID storeId, String normalizedName, String sku, UUID excludeId) {
        if (productRepository.existsByNormalizedName(storeId, normalizedName, excludeId)) {
            throw new ConflictException("Já existe um produto com esse nome nesta loja.");
        }
        if (sku != null && productRepository.existsBySku(storeId, sku, excludeId)) {
            throw new ConflictException("Já existe um produto com esse SKU nesta loja.");
        }
    }

    private String cleanName(String value) {
        String name = TextNormalizer.cleanText(value);
        if (name == null) {
            throw new BusinessRuleException("Informe o nome do produto.");
        }
        if (name.length() > 80) {
            throw new BusinessRuleException("O nome deve ter no máximo 80 caracteres.");
        }
        return name;
    }

    private String cleanSku(String value) {
        String sku = TextNormalizer.cleanSku(value);
        if (sku != null && sku.length() > 40) {
            throw new BusinessRuleException("O SKU deve ter no máximo 40 caracteres.");
        }
        return sku;
    }

    private String cleanCategory(String value) {
        String category = TextNormalizer.cleanText(value);
        return category == null ? "Geral" : category;
    }

    private String cleanUnit(String value) {
        String unit = TextNormalizer.cleanText(value);
        return unit == null ? "un" : unit;
    }
}
