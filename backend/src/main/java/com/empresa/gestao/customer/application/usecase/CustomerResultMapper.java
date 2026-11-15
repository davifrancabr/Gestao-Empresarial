package com.empresa.gestao.customer.application.usecase;

import com.empresa.gestao.customer.application.dto.CustomerResult;
import com.empresa.gestao.customer.domain.model.Customer;

final class CustomerResultMapper {

    private CustomerResultMapper() {
    }

    static CustomerResult toResult(Customer customer) {
        return new CustomerResult(customer.id(), customer.storeId(), customer.name(), customer.cpf(),
                customer.phone(), customer.email(), customer.active(), customer.createdAt(),
                customer.updatedAt());
    }
}
