package com.empresa.gestao.movement.application.usecase;

import com.empresa.gestao.movement.application.dto.MovementCommand;
import com.empresa.gestao.movement.application.dto.MovementResult;
import com.empresa.gestao.movement.domain.model.StockMovement;
import com.empresa.gestao.movement.domain.repository.StockMovementRepository;
import com.empresa.gestao.product.application.dto.ProductInfo;
import com.empresa.gestao.product.application.dto.StockChangeResult;
import com.empresa.gestao.product.application.usecase.ApplyStockChangeUseCase;
import com.empresa.gestao.shared.domain.BusinessRuleException;
import com.empresa.gestao.shared.domain.model.MovementType;
import com.empresa.gestao.shared.domain.model.Role;
import com.empresa.gestao.store.application.dto.StoreResult;
import com.empresa.gestao.store.application.port.StoreQueryPort;
import com.empresa.gestao.user.application.dto.UserInfo;
import com.empresa.gestao.user.application.port.UserQueryPort;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordMovementUseCaseTest {

    @Mock
    private StockMovementRepository movementRepository;
    @Mock
    private StoreQueryPort storeQueryPort;
    @Mock
    private UserQueryPort userQueryPort;
    @Mock
    private ApplyStockChangeUseCase applyStockChange;

    private RecordMovementUseCase useCase;
    private final UUID storeId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new RecordMovementUseCase(movementRepository, storeQueryPort, userQueryPort,
                applyStockChange);
        when(storeQueryPort.getRequired(storeId))
                .thenReturn(new StoreResult(storeId, "Loja", null, true, null, null));
        when(userQueryPort.findById(userId)).thenReturn(Optional.of(
                new UserInfo(userId, "Operador", "operador@gestao.local", Role.OPERADOR, true)));
    }

    @Test
    void entryIncreasesStockAndKeepsAuditTrail() {
        stubSave();
        when(applyStockChange.execute(storeId, productId, MovementType.ENTRY, 7, false))
                .thenReturn(change(5, 12, 7));

        MovementResult result = useCase.record(storeId, userId, false,
                new MovementCommand(productId, MovementType.ENTRY, 7, "Compra", "NF 10"));

        assertThat(result.previousQuantity()).isEqualTo(5);
        assertThat(result.resultingQuantity()).isEqualTo(12);
        assertThat(result.quantity()).isEqualTo(7);
        assertThat(result.userName()).isEqualTo("Operador");
        assertThat(result.productName()).isEqualTo("Arroz 5 kg");
        assertThat(result.type()).isEqualTo(MovementType.ENTRY);
        verify(applyStockChange).execute(storeId, productId, MovementType.ENTRY, 7, false);
        verify(movementRepository).save(any(StockMovement.class));
    }

    @Test
    void exitAboveAvailableStockIsRejectedBeforePersisting() {
        when(applyStockChange.execute(storeId, productId, MovementType.EXIT, 6, false))
                .thenThrow(new BusinessRuleException(
                        "Saída indisponível. Há apenas 5 unidades em estoque."));

        assertThatThrownBy(() -> useCase.record(storeId, userId, false,
                new MovementCommand(productId, MovementType.EXIT, 6, "Venda", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Saída indisponível");

        verify(movementRepository, never()).save(any());
    }

    @Test
    void adjustmentRecordsAbsoluteDifference() {
        stubSave();
        when(applyStockChange.execute(storeId, productId, MovementType.ADJUSTMENT, 3, true))
                .thenReturn(change(5, 3, 2));

        MovementResult result = useCase.record(storeId, userId, true,
                new MovementCommand(productId, MovementType.ADJUSTMENT, 3, "Inventário", "Contagem"));

        assertThat(result.previousQuantity()).isEqualTo(5);
        assertThat(result.resultingQuantity()).isEqualTo(3);
        assertThat(result.quantity()).isEqualTo(2);
        assertThat(result.type()).isEqualTo(MovementType.ADJUSTMENT);
    }

    @Test
    void historicalMovementKeepsProvidedTimestamp() {
        stubSave();
        when(applyStockChange.execute(storeId, productId, MovementType.ENTRY, 1, false))
                .thenReturn(change(5, 6, 1));
        Instant occurredAt = Instant.parse("2026-01-10T12:00:00Z");

        MovementResult result = useCase.recordAt(storeId, userId, false,
                new MovementCommand(productId, MovementType.ENTRY, 1, "Carga", null), occurredAt);

        assertThat(result.createdAt()).isEqualTo(occurredAt);
    }

    @Test
    void movementWithoutReasonIsRejectedBeforeChangingStock() {
        assertThatThrownBy(() -> useCase.record(storeId, userId, false,
                new MovementCommand(productId, MovementType.ENTRY, 1, "   ", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Informe o motivo da movimentação.");

        verify(applyStockChange, never()).execute(any(), any(), any(), anyInt(), anyBoolean());
        verify(movementRepository, never()).save(any());
    }

    private void stubSave() {
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement movement = invocation.getArgument(0);
            return new StockMovement(UUID.randomUUID(), movement.storeId(), movement.userId(),
                    movement.productId(), movement.productNameSnapshot(), movement.productSku(),
                    movement.type(), movement.quantity(), movement.previousQuantity(),
                    movement.resultingQuantity(), movement.reason(), movement.observation(),
                    movement.userName(), movement.createdAt());
        });
    }

    private StockChangeResult change(int previous, int resulting, int recorded) {        ProductInfo product = new ProductInfo(productId, storeId, "Arroz 5 kg", "ARROZ-5KG",
                resulting, 2, "un", true, 1L);
        return new StockChangeResult(product, previous, resulting, recorded);
    }
}
