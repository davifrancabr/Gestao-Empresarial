package com.empresa.gestao.movement;

import com.empresa.gestao.movement.dto.MovementResponse;
import org.springframework.stereotype.Component;

@Component
public class MovementMapper {

    public MovementResponse toResponse(StockMovement movement) {
        return new MovementResponse(
                movement.getId(),
                movement.getStore().getId(),
                movement.getProduct().getId(),
                movement.getProductNameSnapshot(),
                movement.getProduct().getSku(),
                movement.getType(),
                movement.getQuantity(),
                movement.getPreviousQuantity(),
                movement.getResultingQuantity(),
                movement.getReason(),
                movement.getObservation(),
                movement.getUser().getName(),
                movement.getCreatedAt()
        );
    }
}
