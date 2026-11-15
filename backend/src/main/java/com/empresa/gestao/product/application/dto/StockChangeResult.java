package com.empresa.gestao.product.application.dto;

public record StockChangeResult(
        ProductInfo product,
        int previousQuantity,
        int resultingQuantity,
        int recordedQuantity
) {
}
