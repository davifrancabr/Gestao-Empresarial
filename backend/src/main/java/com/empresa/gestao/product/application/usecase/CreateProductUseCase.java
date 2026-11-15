package com.empresa.gestao.product.application.usecase;

import com.empresa.gestao.product.application.dto.ProductCreateCommand;
import com.empresa.gestao.product.application.dto.ProductResult;
import com.empresa.gestao.product.application.port.InitialStockPort;
import com.empresa.gestao.product.domain.model.Product;
import com.empresa.gestao.product.domain.repository.ProductRepository;
import com.empresa.gestao.shared.domain.BusinessRuleException;
import com.empresa.gestao.shared.domain.ConflictException;
import com.empresa.gestao.shared.domain.util.TextNormalizer;
import com.empresa.gestao.store.application.port.StoreQueryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CreateProductUseCase {

    private final ProductRepository productRepository;
    private final StoreQueryPort storeQueryPort;
    private final InitialStockPort initialStockPort;

    public CreateProductUseCase(ProductRepository productRepository, StoreQueryPort storeQueryPort,
                                InitialStockPort initialStockPort) {
        this.productRepository = productRepository;
        this.storeQueryPort = storeQueryPort;
        this.initialStockPort = initialStockPort;
    }

    @Transactional
    public ProductResult execute(UUID storeId, UUID userId, ProductCreateCommand command) {
        String name = cleanName(command.name());
        String normalizedName = TextNormalizer.normalizeName(name);
        String sku = cleanSku(command.sku());
        ensureUnique(storeId, normalizedName, sku, null);

        storeQueryPort.getRequired(storeId);

        Product product = new Product(null, storeId, name, normalizedName, sku,
                cleanCategory(command.category()), TextNormalizer.cleanText(command.description()),
                0, command.minimum(), cleanUnit(command.unit()), true, null, null, null);
        product = productRepository.save(product);

        int initial = command.quantity() == null ? 0 : command.quantity();
        if (initial > 0) {
            initialStockPort.recordInitialStock(storeId, userId, product.id(), initial);
        }

        // O saldo inicial altera a versão otimista do produto. Sem o flush a
        // resposta devolveria a versão antiga e a primeira edição falharia com 409.
        productRepository.flush();
        Product refreshed = productRepository.findById(product.id())
                .orElseThrow(() -> new ConflictException("Não foi possível recarregar o produto."));
        return ProductResultMapper.toResult(refreshed);
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
