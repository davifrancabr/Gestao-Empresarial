package com.empresa.gestao.sale.application.usecase;

import com.empresa.gestao.sale.application.dto.SaleItemResult;
import com.empresa.gestao.sale.application.dto.SaleResult;
import com.empresa.gestao.sale.application.dto.SaleSummaryResult;
import com.empresa.gestao.sale.domain.model.Sale;

final class SaleResultMapper {

    private SaleResultMapper() {
    }

    static SaleSummaryResult toSummary(Sale sale) {
        return new SaleSummaryResult(
                sale.id(),
                sale.customerId(),
                sale.customerId() == null ? "Consumidor final" : sale.customerName(),
                sale.userName(),
                sale.items().size(),
                sale.total(),
                sale.status(),
                sale.createdAt()
        );
    }

    static SaleResult toResult(Sale sale) {
        var items = sale.items().stream()
                .map(item -> new SaleItemResult(item.id(), item.productId(),
                        item.productNameSnapshot(), item.quantity(), item.unitPrice(),
                        item.subtotal()))
                .toList();
        return new SaleResult(
                sale.id(),
                sale.storeId(),
                sale.customerId(),
                sale.customerId() == null ? "Consumidor final" : sale.customerName(),
                sale.userId(),
                sale.userName(),
                sale.total(),
                sale.status(),
                sale.createdAt(),
                items
        );
    }
}
