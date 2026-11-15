package com.empresa.gestao.product.application.usecase;

import com.empresa.gestao.product.application.dto.StockChangeResult;
import com.empresa.gestao.product.domain.model.Product;
import com.empresa.gestao.product.domain.model.StockChange;
import com.empresa.gestao.product.domain.repository.ProductRepository;
import com.empresa.gestao.shared.domain.BusinessRuleException;
import com.empresa.gestao.shared.domain.ForbiddenException;
import com.empresa.gestao.shared.domain.NotFoundException;
import com.empresa.gestao.shared.domain.model.MovementType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ApplyStockChangeUseCase {

    private final ProductRepository productRepository;

    public ApplyStockChangeUseCase(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public StockChangeResult execute(UUID storeId, UUID productId, MovementType type,
                                     int requested, boolean allowAdjustment) {
        Product product = productRepository.findByIdAndStoreIdAndActiveTrue(productId, storeId)
                .orElseThrow(() -> new NotFoundException("Produto não encontrado ou arquivado."));

        StockChange change = switch (type) {
            case ENTRY -> product.registerEntry(requested);
            case EXIT -> product.registerExit(requested);
            case ADJUSTMENT -> {
                if (!allowAdjustment) {
                    throw new ForbiddenException(
                            "Apenas administradores e gerentes podem realizar ajustes.");
                }
                yield product.adjustTo(requested);
            }
        };

        Product saved = productRepository.save(product.withBalance(change.resultingQuantity()));
        return new StockChangeResult(ProductResultMapper.toInfo(saved),
                change.previousQuantity(), change.resultingQuantity(), change.recordedQuantity());
    }
}
