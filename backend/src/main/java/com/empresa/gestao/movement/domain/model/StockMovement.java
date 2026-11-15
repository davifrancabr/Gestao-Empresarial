package com.empresa.gestao.movement.domain.model;

import com.empresa.gestao.shared.domain.model.MovementType;

import java.time.Instant;
import java.util.UUID;

public final class StockMovement {

    private final UUID id;
    private final UUID storeId;
    private final UUID userId;
    private final UUID productId;
    private final String productNameSnapshot;
    private final String productSku;
    private final MovementType type;
    private final int quantity;
    private final int previousQuantity;
    private final int resultingQuantity;
    private final String reason;
    private final String observation;
    private final String userName;
    private final Instant createdAt;

    public StockMovement(UUID id, UUID storeId, UUID userId, UUID productId, String productNameSnapshot,
                         String productSku, MovementType type, int quantity, int previousQuantity,
                         int resultingQuantity, String reason, String observation, String userName,
                         Instant createdAt) {
        this.id = id;
        this.storeId = storeId;
        this.userId = userId;
        this.productId = productId;
        this.productNameSnapshot = productNameSnapshot;
        this.productSku = productSku;
        this.type = type;
        this.quantity = quantity;
        this.previousQuantity = previousQuantity;
        this.resultingQuantity = resultingQuantity;
        this.reason = reason;
        this.observation = observation;
        this.userName = userName;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public UUID storeId() {
        return storeId;
    }

    public UUID userId() {
        return userId;
    }

    public UUID productId() {
        return productId;
    }

    public String productNameSnapshot() {
        return productNameSnapshot;
    }

    public String productSku() {
        return productSku;
    }

    public MovementType type() {
        return type;
    }

    public int quantity() {
        return quantity;
    }

    public int previousQuantity() {
        return previousQuantity;
    }

    public int resultingQuantity() {
        return resultingQuantity;
    }

    public String reason() {
        return reason;
    }

    public String observation() {
        return observation;
    }

    public String userName() {
        return userName;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
