package com.empresa.gestao.customer.application.usecase;

import com.empresa.gestao.customer.application.dto.CustomerResult;
import com.empresa.gestao.customer.domain.model.Customer;
import com.empresa.gestao.customer.domain.repository.CustomerRepository;
import com.empresa.gestao.shared.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetCustomerUseCase {

    private final CustomerRepository customerRepository;

    public GetCustomerUseCase(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public CustomerResult execute(UUID storeId, UUID id) {
        Customer customer = customerRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
        return CustomerResultMapper.toResult(customer);
    }
}
