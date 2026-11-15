package com.empresa.gestao.product.application.port;

import java.util.UUID;

/**
 * Porta implementada pelo módulo de movimentações para registrar o saldo
 * inicial na mesma transação do cadastro do produto.
 */
public interface InitialStockPort {

    void recordInitialStock(UUID storeId, UUID userId, UUID productId, int quantity);
}
