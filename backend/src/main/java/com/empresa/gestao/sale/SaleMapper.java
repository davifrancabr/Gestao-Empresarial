package com.empresa.gestao.sale;

import com.empresa.gestao.sale.dto.SaleItemResponse;
import com.empresa.gestao.sale.dto.SaleResponse;
import com.empresa.gestao.sale.dto.SaleSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SaleMapper {

    public SaleSummaryResponse toSummary(Sale sale) {
        return new SaleSummaryResponse(
                sale.getId(),
                sale.getCustomer() == null ? null : sale.getCustomer().getId(),
                sale.getCustomer() == null ? "Consumidor final" : sale.getCustomer().getName(),
                sale.getUser().getName(),
                sale.getItems().size(),
                sale.getTotal(),
                sale.getStatus(),
                sale.getCreatedAt()
        );
    }

    public SaleResponse toResponse(Sale sale) {
        List<SaleItemResponse> items = sale.getItems().stream()
                .map(item -> new SaleItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProductNameSnapshot(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()))
                .toList();
        return new SaleResponse(
                sale.getId(),
                sale.getStore().getId(),
                sale.getCustomer() == null ? null : sale.getCustomer().getId(),
                sale.getCustomer() == null ? "Consumidor final" : sale.getCustomer().getName(),
                sale.getUser().getId(),
                sale.getUser().getName(),
                sale.getTotal(),
                sale.getStatus(),
                sale.getCreatedAt(),
                items
        );
    }
}
