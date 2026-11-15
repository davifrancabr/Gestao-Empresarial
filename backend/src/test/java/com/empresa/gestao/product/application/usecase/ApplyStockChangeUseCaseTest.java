package com.empresa.gestao.product.application.usecase;

import com.empresa.gestao.product.application.dto.StockChangeResult;
import com.empresa.gestao.product.domain.model.Product;
import com.empresa.gestao.product.domain.repository.ProductRepository;
import com.empresa.gestao.shared.domain.BusinessRuleException;
import com.empresa.gestao.shared.domain.ForbiddenException;
import com.empresa.gestao.shared.domain.model.MovementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplyStockChangeUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    private ApplyStockChangeUseCase useCase;
    private final UUID storeId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new ApplyStockChangeUseCase(productRepository);
    }

    @Test
    void entryPersistsNewBalance() {
        when(productRepository.findByIdAndStoreIdAndActiveTrue(productId, storeId))
                .thenReturn(Optional.of(product(5)));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StockChangeResult result = useCase.execute(storeId, productId, MovementType.ENTRY, 7, false);

        assertThat(result.previousQuantity()).isEqualTo(5);
        assertThat(result.resultingQuantity()).isEqualTo(12);
        assertThat(result.recordedQuantity()).isEqualTo(7);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void adjustmentRequiresManagementPermission() {
        when(productRepository.findByIdAndStoreIdAndActiveTrue(productId, storeId))
                .thenReturn(Optional.of(product(5)));

        assertThatThrownBy(() -> useCase.execute(storeId, productId, MovementType.ADJUSTMENT, 3, false))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Apenas administradores e gerentes podem realizar ajustes.");

        verify(productRepository, never()).save(any());
    }

    @Test
    void exitAboveBalanceDoesNotPersist() {
        when(productRepository.findByIdAndStoreIdAndActiveTrue(productId, storeId))
                .thenReturn(Optional.of(product(5)));

        assertThatThrownBy(() -> useCase.execute(storeId, productId, MovementType.EXIT, 6, false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Saída indisponível");

        verify(productRepository, never()).save(any());
    }

    private Product product(int quantity) {
        return new Product(productId, storeId, "Arroz 5 kg", "arroz 5 kg", "ARROZ-5KG", "Mercearia",
                null, quantity, 2, "un", true, 0L, null, null);
    }
}
