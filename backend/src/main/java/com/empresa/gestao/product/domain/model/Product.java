package com.empresa.gestao.product.domain.model;

import com.empresa.gestao.shared.domain.BusinessRuleException;
import com.empresa.gestao.shared.domain.model.ProductStatus;

import java.time.Instant;
import java.util.UUID;

public final class Product {

    private final UUID id;
    private final UUID storeId;
    private final String name;
    private final String normalizedName;
    private final String sku;
    private final String category;
    private final String description;
    private final int quantity;
    private final int minimum;
    private final String unit;
    private final boolean active;
    private final Long version;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Product(UUID id, UUID storeId, String name, String normalizedName, String sku,
                   String category, String description, int quantity, int minimum, String unit,
                   boolean active, Long version, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.storeId = storeId;
        this.name = name;
        this.normalizedName = normalizedName;
        this.sku = sku;
        this.category = category;
        this.description = description;
        this.quantity = quantity;
        this.minimum = minimum;
        this.unit = unit;
        this.active = active;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Product withBalance(int newQuantity) {
        return new Product(id, storeId, name, normalizedName, sku, category, description,
                newQuantity, minimum, unit, active, version, createdAt, updatedAt);
    }

    public Product withDetails(String newName, String newNormalizedName, String newSku,
                               String newCategory, String newDescription, int newMinimum,
                               String newUnit) {
        return new Product(id, storeId, newName, newNormalizedName, newSku, newCategory,
                newDescription, quantity, newMinimum, newUnit, active, version, createdAt, updatedAt);
    }

    public Product archived() {
        return new Product(id, storeId, name, normalizedName, sku, category, description,
                quantity, minimum, unit, false, version, createdAt, updatedAt);
    }

    public StockChange registerEntry(int requested) {
        requirePositive(requested);
        int previous = quantity;
        int resulting = previous + requested;
        return new StockChange(previous, resulting, requested);
    }

    public StockChange registerExit(int requested) {
        requirePositive(requested);
        if (requested > quantity) {
            throw new BusinessRuleException(
                    "Saída indisponível. Há apenas " + quantity
                            + (quantity == 1 ? " unidade" : " unidades") + " em estoque.");
        }
        int previous = quantity;
        int resulting = previous - requested;
        return new StockChange(previous, resulting, requested);
    }

    public StockChange adjustTo(int target) {
        if (target < 0) {
            throw new BusinessRuleException("O saldo ajustado não pode ser negativo.");
        }
        if (target == quantity) {
            throw new BusinessRuleException("O saldo informado é igual ao saldo atual.");
        }
        int previous = quantity;
        return new StockChange(previous, target, Math.abs(target - previous));
    }

    private void requirePositive(int requested) {
        if (requested <= 0) {
            throw new BusinessRuleException("Informe uma quantidade inteira maior que zero.");
        }
    }

    public ProductStatus status() {
        if (quantity == 0) {
            return ProductStatus.OUT_OF_STOCK;
        }
        if (quantity <= minimum) {
            return ProductStatus.LOW;
        }
        return ProductStatus.NORMAL;
    }

    public UUID id() {
        return id;
    }

    public UUID storeId() {
        return storeId;
    }

    public String name() {
        return name;
    }

    public String normalizedName() {
        return normalizedName;
    }

    public String sku() {
        return sku;
    }

    public String category() {
        return category;
    }

    public String description() {
        return description;
    }

    public int quantity() {
        return quantity;
    }

    public int minimum() {
        return minimum;
    }

    public String unit() {
        return unit;
    }

    public boolean active() {
        return active;
    }

    public Long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
