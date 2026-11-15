package com.empresa.gestao.movement.domain.repository;

import java.util.UUID;

/**
 * Projeção de leitura devolvida pelas consultas de produtos mais movimentados.
 */
public record TopMovedProduct(
        UUID productId,
        String name,
        long totalQuantity,
        long movementCount
) {
}
