package com.empresa.gestao.movement.infrastructure.persistence.mapper;

import com.empresa.gestao.movement.domain.model.StockMovement;
import com.empresa.gestao.movement.infrastructure.persistence.entity.StockMovementJpaEntity;

public final class StockMovementPersistenceMapper {

    private StockMovementPersistenceMapper() {
    }

    public static StockMovement toDomain(StockMovementJpaEntity entity, String productSku, String userName) {
        return new StockMovement(entity.getId(), entity.getStoreId(), entity.getUserId(), entity.getProductId(),
                entity.getProductNameSnapshot(), productSku, entity.getType(), entity.getQuantity(),
                entity.getPreviousQuantity(), entity.getResultingQuantity(), entity.getReason(),
                entity.getObservation(), userName, entity.getCreatedAt());
    }

    public static StockMovementJpaEntity toEntity(StockMovement movement) {
        StockMovementJpaEntity entity = new StockMovementJpaEntity();
        entity.setStoreId(movement.storeId());
        entity.setProductId(movement.productId());
        entity.setProductNameSnapshot(movement.productNameSnapshot());
        entity.setType(movement.type());
        entity.setQuantity(movement.quantity());
        entity.setPreviousQuantity(movement.previousQuantity());
        entity.setResultingQuantity(movement.resultingQuantity());
        entity.setReason(movement.reason());
        entity.setObservation(movement.observation());
        entity.setUserId(movement.userId());
        entity.setCreatedAt(movement.createdAt());
        return entity;
    }
}
