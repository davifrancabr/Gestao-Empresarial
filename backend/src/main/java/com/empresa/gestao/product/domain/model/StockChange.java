package com.empresa.gestao.product.domain.model;

/**
 * Resultado de uma alteração de saldo: saldo anterior, saldo resultante e
 * quantidade efetivamente registrada no histórico (no ajuste, a diferença).
 */
public record StockChange(int previousQuantity, int resultingQuantity, int recordedQuantity) {
}
