package com.empresa.gestao.sale.domain.model;

import com.empresa.gestao.shared.domain.model.SaleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Sale {

    private final UUID id;
    private final UUID storeId;
    private final UUID customerId;
    private final String customerName;
    private final UUID userId;
    private final String userName;
    private final BigDecimal total;
    private final SaleStatus status;
    private final Instant createdAt;
    private final List<SaleItem> items;

    public Sale(UUID id, UUID storeId, UUID customerId, String customerName, UUID userId,
                String userName, BigDecimal total, SaleStatus status, Instant createdAt,
                List<SaleItem> items) {
        this.id = id;
        this.storeId = storeId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.userId = userId;
        this.userName = userName;
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
        this.items = List.copyOf(items);
    }

    public Sale withTotal(BigDecimal newTotal) {
        return new Sale(id, storeId, customerId, customerName, userId, userName, newTotal,
                status, createdAt, items);
    }

    public Sale withNames(String newCustomerName, String newUserName) {
        return new Sale(id, storeId, customerId, newCustomerName, userId, newUserName, total,
                status, createdAt, items);
    }

    public UUID id() {
        return id;
    }

    public UUID storeId() {
        return storeId;
    }

    public UUID customerId() {
        return customerId;
    }

    public String customerName() {
        return customerName;
    }

    public UUID userId() {
        return userId;
    }

    public String userName() {
        return userName;
    }

    public BigDecimal total() {
        return total;
    }

    public SaleStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public List<SaleItem> items() {
        return items;
    }
}
