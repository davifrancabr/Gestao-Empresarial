package com.empresa.gestao.product;

import com.empresa.gestao.common.exception.ConflictException;
import com.empresa.gestao.movement.MovementService;
import com.empresa.gestao.movement.MovementType;
import com.empresa.gestao.product.dto.ProductCreateRequest;
import com.empresa.gestao.product.dto.ProductResponse;
import com.empresa.gestao.product.dto.ProductUpdateRequest;
import com.empresa.gestao.store.Store;
import com.empresa.gestao.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private MovementService movementService;

    private ProductService productService;
    private final ProductMapper mapper = new ProductMapper();
    private final UUID storeId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private Store store;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, storeRepository, movementService, mapper);
        store = new Store();
        store.setId(storeId);
        store.setName("Loja Teste");
    }

    @Test
    void createsProductWithNormalizedNameAndInitialMovement() {
        UUID productId = UUID.randomUUID();
        AtomicReference<Product> saved = new AtomicReference<>();
        when(productRepository.existsByStoreIdAndNormalizedNameAndActiveTrue(storeId, "arroz 5 kg"))
                .thenReturn(false);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(productId);
            saved.set(product);
            return product;
        });
        when(productRepository.findById(productId)).thenAnswer(invocation -> Optional.of(saved.get()));

        ProductResponse response = productService.create(storeId, userId, new ProductCreateRequest(
                "  Arroz   5 kg  ", "arroz-5kg", "Mercearia", null, 5, 2, "un"));

        assertThat(response.name()).isEqualTo("Arroz 5 kg");
        assertThat(response.sku()).isEqualTo("ARROZ-5KG");
        assertThat(saved.get().getNormalizedName()).isEqualTo("arroz 5 kg");
        verify(productRepository).flush();
        verify(movementService).record(eq(storeId), eq(userId), eq(false),
                argThat(movement -> movement.type() == MovementType.ENTRY
                        && movement.quantity() == 5
                        && movement.reason().equals("Saldo inicial")));
    }

    @Test
    void flushesAfterUpdateSoResponseCarriesFreshVersion() {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setStore(store);
        product.setName("Arroz 5 kg");
        product.setNormalizedName("arroz 5 kg");
        product.setVersion(2L);
        product.setActive(true);
        when(productRepository.findByIdAndStoreIdAndActiveTrue(productId, storeId))
                .thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenAnswer(invocation -> {
            product.setVersion(3L);
            return product;
        });

        ProductResponse response = productService.update(storeId, productId, new ProductUpdateRequest(
                "Arroz 5 kg", null, "Mercearia", null, 2, "un", 2L));

        assertThat(response.version()).isEqualTo(3L);
        verify(productRepository).flush();
    }

    @Test
    void rejectsDuplicatedNameInSameStore() {
        when(productRepository.existsByStoreIdAndNormalizedNameAndActiveTrue(storeId, "arroz 5 kg"))
                .thenReturn(true);

        assertThatThrownBy(() -> productService.create(storeId, userId, new ProductCreateRequest(
                "Arroz 5 kg", null, "Mercearia", null, 0, 2, "un")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Já existe um produto");

        verify(productRepository, never()).save(any());
        verify(movementService, never()).record(any(), any(), eq(false), any());
    }

    @Test
    void rejectsStaleVersionOnUpdate() {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setStore(store);
        product.setName("Arroz 5 kg");
        product.setNormalizedName("arroz 5 kg");
        product.setVersion(3L);
        product.setActive(true);
        when(productRepository.findByIdAndStoreIdAndActiveTrue(productId, storeId))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.update(storeId, productId, new ProductUpdateRequest(
                "Arroz 5 kg", null, "Mercearia", null, 2, "un", 2L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("alterado por outra operação");

        verify(productRepository, never()).save(any());
    }

    @Test
    void archivesInsteadOfDeleting() {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setStore(store);
        product.setActive(true);
        when(productRepository.findByIdAndStoreIdAndActiveTrue(productId, storeId))
                .thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        productService.archive(storeId, productId);

        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
        verify(productRepository, never()).delete(any(Product.class));
    }
}
