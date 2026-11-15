package com.empresa.gestao.sale.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public final class SaleItem {

    private final UUID id;
    private final UUID productId;
    private final String productNameSnapshot;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal subtotal;

    public SaleItem(UUID id, UUID productId, String productNameSnapshot, int quantity,
                    BigDecimal unitPrice, BigDecimal subtotal) {
        this.id = id;
        this.productId = productId;
        this.productNameSnapshot = productNameSnapshot;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public UUID id() {
        return id;
    }

    public UUID productId() {
        return productId;
    }

    public String productNameSnapshot() {
        return productNameSnapshot;
    }

    public int quantity() {
        return quantity;
    }

    public BigDecimal unitPrice() {
        return unitPrice;
    }

    public BigDecimal subtotal() {
        return subtotal;
    }
}
