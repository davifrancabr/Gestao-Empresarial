package com.empresa.gestao.product.application.usecase;

import com.empresa.gestao.product.application.dto.ProductCreateCommand;
import com.empresa.gestao.product.application.dto.ProductResult;
import com.empresa.gestao.product.application.port.InitialStockPort;
import com.empresa.gestao.product.domain.model.Product;
import com.empresa.gestao.product.domain.repository.ProductRepository;
import com.empresa.gestao.shared.domain.ConflictException;
import com.empresa.gestao.shared.domain.model.ProductStatus;
import com.empresa.gestao.store.application.dto.StoreResult;
import com.empresa.gestao.store.application.port.StoreQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateProductUseCaseTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private StoreQueryPort storeQueryPort;
    @Mock
    private InitialStockPort initialStockPort;

    private CreateProductUseCase useCase;
    private final UUID storeId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new CreateProductUseCase(productRepository, storeQueryPort, initialStockPort);
    }

    @Test
    void createsProductWithNormalizedNameAndInitialStockEntry() {
        UUID productId = UUID.randomUUID();
        when(productRepository.existsByNormalizedName(storeId, "arroz 5 kg", null)).thenReturn(false);
        when(productRepository.existsBySku(storeId, "ARROZ-5KG", null)).thenReturn(false);
        when(storeQueryPort.getRequired(storeId))
                .thenReturn(new StoreResult(storeId, "Loja Teste", null, true, null, null));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            return new Product(productId, product.storeId(), product.name(), product.normalizedName(),
                    product.sku(), product.category(), product.description(), product.quantity(),
                    product.minimum(), product.unit(), product.active(), 0L, Instant.now(), Instant.now());
        });
        when(productRepository.findById(productId)).thenAnswer(invocation -> Optional.of(
                new Product(productId, storeId, "Arroz 5 kg", "arroz 5 kg", "ARROZ-5KG", "Mercearia",
                        null, 5, 2, "un", true, 1L, Instant.now(), Instant.now())));

        ProductResult result = useCase.execute(storeId, userId, command(
                "  Arroz   5 kg  ", "arroz-5kg", "Mercearia", null, 5, 2, "un"));

        assertThat(result.name()).isEqualTo("Arroz 5 kg");
        assertThat(result.sku()).isEqualTo("ARROZ-5KG");
        assertThat(result.quantity()).isEqualTo(5);
        assertThat(result.status()).isEqualTo(ProductStatus.NORMAL);
        verify(initialStockPort).recordInitialStock(storeId, userId, productId, 5);
        verify(productRepository).flush();
    }

    @Test
    void doesNotRegisterInitialMovementWhenQuantityIsZero() {
        UUID productId = UUID.randomUUID();
        when(productRepository.existsByNormalizedName(storeId, "arroz 5 kg", null)).thenReturn(false);
        when(storeQueryPort.getRequired(storeId))
                .thenReturn(new StoreResult(storeId, "Loja Teste", null, true, null, null));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            return new Product(productId, product.storeId(), product.name(), product.normalizedName(),
                    product.sku(), product.category(), product.description(), product.quantity(),
                    product.minimum(), product.unit(), product.active(), 0L, null, null);
        });
        when(productRepository.findById(productId)).thenAnswer(invocation -> Optional.of(
                new Product(productId, storeId, "Arroz 5 kg", "arroz 5 kg", null, "Mercearia",
                        null, 0, 2, "un", true, 0L, null, null)));

        useCase.execute(storeId, userId, command("Arroz 5 kg", null, null, null, 0, 2, null));

        verify(initialStockPort, never()).recordInitialStock(any(), any(), any(), anyInt());
    }

    @Test
    void rejectsDuplicatedNameInSameStore() {
        when(productRepository.existsByNormalizedName(storeId, "arroz 5 kg", null)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(storeId, userId,
                command("Arroz 5 kg", null, "Mercearia", null, 0, 2, "un")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Já existe um produto com esse nome");

        verify(productRepository, never()).save(any());
        verify(initialStockPort, never()).recordInitialStock(any(), any(), any(), anyInt());
    }

    @Test
    void rejectsDuplicatedSkuInSameStore() {
        when(productRepository.existsByNormalizedName(storeId, "arroz 5 kg", null)).thenReturn(false);
        when(productRepository.existsBySku(storeId, "ARROZ-5KG", null)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(storeId, userId,
                command("Arroz 5 kg", "arroz-5kg", "Mercearia", null, 0, 2, "un")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Já existe um produto com esse SKU");

        verify(productRepository, never()).save(any());
    }

    private ProductCreateCommand command(String name, String sku, String category, String description,
                                         Integer quantity, Integer minimum, String unit) {
        return new ProductCreateCommand(name, sku, category, description, quantity, minimum, unit);
    }
}
