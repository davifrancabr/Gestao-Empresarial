package com.empresa.gestao.movement;

import com.empresa.gestao.common.exception.BusinessRuleException;
import com.empresa.gestao.common.exception.ForbiddenException;
import com.empresa.gestao.movement.dto.MovementRequest;
import com.empresa.gestao.movement.dto.MovementResponse;
import com.empresa.gestao.product.Product;
import com.empresa.gestao.product.ProductRepository;
import com.empresa.gestao.store.Store;
import com.empresa.gestao.store.StoreRepository;
import com.empresa.gestao.user.User;
import com.empresa.gestao.user.UserRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovementServiceTest {

    @Mock
    private StockMovementRepository movementRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private UserRepository userRepository;

    private MovementService movementService;
    private final UUID storeId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private Store store;
    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        movementService = new MovementService(movementRepository, productRepository,
                storeRepository, userRepository, new MovementMapper());
        store = new Store();
        store.setId(storeId);
        store.setName("Loja");
        user = new User();
        user.setId(userId);
        user.setName("Operador");
        product = new Product();
        product.setId(productId);
        product.setStore(store);
        product.setName("Arroz 5 kg");
        product.setSku("ARROZ-5KG");
        product.setQuantity(5);
        product.setMinimum(2);
        product.setActive(true);

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findByIdAndStoreIdAndActiveTrue(productId, storeId))
                .thenReturn(Optional.of(product));
        org.mockito.Mockito.lenient().when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(movementRepository.save(any(StockMovement.class)))
                .thenAnswer(invocation -> {
                    StockMovement movement = invocation.getArgument(0);
                    movement.setId(UUID.randomUUID());
                    return movement;
                });
    }

    @Test
    void entryIncreasesStockAndKeepsAuditTrail() {
        MovementResponse response = movementService.record(storeId, userId, false,
                new MovementRequest(productId, MovementType.ENTRY, 7, "Compra", "NF 10"));

        assertThat(product.getQuantity()).isEqualTo(12);
        assertThat(response.previousQuantity()).isEqualTo(5);
        assertThat(response.resultingQuantity()).isEqualTo(12);
        assertThat(response.quantity()).isEqualTo(7);
        assertThat(response.userName()).isEqualTo("Operador");
        assertThat(response.productName()).isEqualTo("Arroz 5 kg");
        verify(productRepository).save(product);
    }

    @Test
    void exitCannotExceedAvailableStock() {
        assertThatThrownBy(() -> movementService.record(storeId, userId, false,
                new MovementRequest(productId, MovementType.EXIT, 6, "Venda", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Saída indisponível");

        assertThat(product.getQuantity()).isEqualTo(5);
        verify(productRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void adjustmentRequiresManagementPermission() {
        assertThatThrownBy(() -> movementService.record(storeId, userId, false,
                new MovementRequest(productId, MovementType.ADJUSTMENT, 3, "Inventário", null)))
                .isInstanceOf(ForbiddenException.class);

        assertThat(product.getQuantity()).isEqualTo(5);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void adjustmentSetsTargetBalanceAndRecordsDifference() {
        MovementResponse response = movementService.record(storeId, userId, true,
                new MovementRequest(productId, MovementType.ADJUSTMENT, 3, "Inventário", "Contagem"));

        assertThat(product.getQuantity()).isEqualTo(3);
        assertThat(response.previousQuantity()).isEqualTo(5);
        assertThat(response.resultingQuantity()).isEqualTo(3);
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.type()).isEqualTo(MovementType.ADJUSTMENT);
    }

    @Test
    void historicalMovementKeepsProvidedTimestamp() {
        Instant occurredAt = Instant.parse("2026-01-10T12:00:00Z");

        MovementResponse response = movementService.recordAt(storeId, userId, false,
                new MovementRequest(productId, MovementType.ENTRY, 1, "Carga", null), occurredAt);

        assertThat(response.createdAt()).isEqualTo(occurredAt);
    }
}
