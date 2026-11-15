package com.empresa.gestao.movement.application.usecase;

import com.empresa.gestao.movement.application.dto.MovementCommand;
import com.empresa.gestao.movement.application.dto.MovementResult;
import com.empresa.gestao.movement.domain.model.StockMovement;
import com.empresa.gestao.movement.domain.repository.StockMovementRepository;
import com.empresa.gestao.product.application.dto.StockChangeResult;
import com.empresa.gestao.product.application.usecase.ApplyStockChangeUseCase;
import com.empresa.gestao.shared.domain.BusinessRuleException;
import com.empresa.gestao.shared.domain.NotFoundException;
import com.empresa.gestao.shared.domain.util.TextNormalizer;
import com.empresa.gestao.store.application.port.StoreQueryPort;
import com.empresa.gestao.user.application.dto.UserInfo;
import com.empresa.gestao.user.application.port.UserQueryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RecordMovementUseCase {

    private final StockMovementRepository movementRepository;
    private final StoreQueryPort storeQueryPort;
    private final UserQueryPort userQueryPort;
    private final ApplyStockChangeUseCase applyStockChange;

    public RecordMovementUseCase(StockMovementRepository movementRepository, StoreQueryPort storeQueryPort,
                                 UserQueryPort userQueryPort, ApplyStockChangeUseCase applyStockChange) {
        this.movementRepository = movementRepository;
        this.storeQueryPort = storeQueryPort;
        this.userQueryPort = userQueryPort;
        this.applyStockChange = applyStockChange;
    }

    @Transactional
    public MovementResult record(UUID storeId, UUID userId, boolean allowAdjustment,
                                 MovementCommand command) {
        return recordAt(storeId, userId, allowAdjustment, command, null);
    }

    /**
     * Registra uma movimentação com data controlada. Usado apenas pela carga de
     * dados de demonstração para produzir histórico coerente.
     */
    @Transactional
    public MovementResult recordAt(UUID storeId, UUID userId, boolean allowAdjustment,
                                   MovementCommand command, Instant occurredAt) {
        storeQueryPort.getRequired(storeId);
        UserInfo user = userQueryPort.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));

        String reason = TextNormalizer.cleanText(command.reason());
        if (reason == null) {
            throw new BusinessRuleException("Informe o motivo da movimentação.");
        }

        StockChangeResult change = applyStockChange.execute(storeId, command.productId(), command.type(),
                command.quantity(), allowAdjustment);

        StockMovement movement = new StockMovement(null, storeId, userId, command.productId(),
                change.product().name(), change.product().sku(), command.type(),
                change.recordedQuantity(), change.previousQuantity(), change.resultingQuantity(),
                reason, TextNormalizer.cleanText(command.observation()), user.name(), occurredAt);
        return MovementResultMapper.toResult(movementRepository.save(movement));
    }
}
