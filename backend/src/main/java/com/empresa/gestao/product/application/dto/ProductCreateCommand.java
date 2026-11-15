package com.empresa.gestao.product.application.dto;

public record ProductCreateCommand(
        String name,
        String sku,
        String category,
        String description,
        Integer quantity,
        Integer minimum,
        String unit
) {
}
