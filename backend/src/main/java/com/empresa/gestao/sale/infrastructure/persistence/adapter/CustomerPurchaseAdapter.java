package com.empresa.gestao.sale.infrastructure.persistence.adapter;

import com.empresa.gestao.customer.application.dto.CustomerPurchaseResult;
import com.empresa.gestao.customer.application.port.CustomerPurchasePort;
import com.empresa.gestao.sale.application.usecase.ListSalesByCustomerUseCase;
import com.empresa.gestao.shared.domain.query.PageResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CustomerPurchaseAdapter implements CustomerPurchasePort {

    private final ListSalesByCustomerUseCase listSalesByCustomer;

    public CustomerPurchaseAdapter(ListSalesByCustomerUseCase listSalesByCustomer) {
        this.listSalesByCustomer = listSalesByCustomer;
    }

    @Override
    public PageResult<CustomerPurchaseResult> findByCustomer(UUID storeId, UUID customerId,
                                                             Integer page, Integer size) {
        return listSalesByCustomer.execute(storeId, customerId, page, size)
                .map(summary -> new CustomerPurchaseResult(summary.id(), summary.customerId(),
                        summary.customerName(), summary.userName(), summary.itemCount(),
                        summary.total(), summary.status(), summary.createdAt()));
    }
}
