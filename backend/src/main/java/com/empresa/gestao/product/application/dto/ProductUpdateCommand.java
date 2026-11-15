package com.empresa.gestao.product.application.dto;

public record ProductUpdateCommand(
        String name,
        String sku,
        String category,
        String description,
        Integer minimum,
        String unit,
        Long version
) {
}
