package com.empresa.gestao.movement.infrastructure.persistence.adapter;

import com.empresa.gestao.movement.application.dto.MovementCommand;
import com.empresa.gestao.movement.application.usecase.RecordMovementUseCase;
import com.empresa.gestao.product.application.port.InitialStockPort;
import com.empresa.gestao.shared.domain.model.MovementType;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InitialStockAdapter implements InitialStockPort {

    private final RecordMovementUseCase recordMovement;

    public InitialStockAdapter(RecordMovementUseCase recordMovement) {
        this.recordMovement = recordMovement;
    }

    @Override
    public void recordInitialStock(UUID storeId, UUID userId, UUID productId, int quantity) {
        recordMovement.recordAt(storeId, userId, false, new MovementCommand(productId,
                MovementType.ENTRY, quantity, "Saldo inicial", "Cadastro inicial do produto"), null);
    }
}
