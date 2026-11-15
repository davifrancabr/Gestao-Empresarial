package com.empresa.gestao.movement.application.usecase;

import com.empresa.gestao.movement.application.dto.MovementResult;
import com.empresa.gestao.movement.domain.model.StockMovement;

final class MovementResultMapper {

    private MovementResultMapper() {
    }

    static MovementResult toResult(StockMovement movement) {
        return new MovementResult(
                movement.id(),
                movement.storeId(),
                movement.productId(),
                movement.productNameSnapshot(),
                movement.productSku(),
                movement.type(),
                movement.quantity(),
                movement.previousQuantity(),
                movement.resultingQuantity(),
                movement.reason(),
                movement.observation(),
                movement.userName(),
                movement.createdAt()
        );
    }
}
