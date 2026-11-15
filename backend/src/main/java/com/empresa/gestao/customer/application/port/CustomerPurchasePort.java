package com.empresa.gestao.customer.application.port;

import com.empresa.gestao.customer.application.dto.CustomerPurchaseResult;
import com.empresa.gestao.shared.domain.query.PageResult;

import java.util.UUID;

/**
 * Porta implementada pelo módulo de vendas para listar compras do cliente.
 */
public interface CustomerPurchasePort {

    PageResult<CustomerPurchaseResult> findByCustomer(UUID storeId, UUID customerId,
                                                      Integer page, Integer size);
}
