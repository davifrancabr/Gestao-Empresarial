package com.empresa.gestao.customer.application.usecase;

import com.empresa.gestao.customer.domain.model.Customer;
import com.empresa.gestao.customer.domain.repository.CustomerRepository;
import com.empresa.gestao.shared.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ArchiveCustomerUseCase {

    private final CustomerRepository customerRepository;

    public ArchiveCustomerUseCase(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public void execute(UUID storeId, UUID id) {
        Customer customer = customerRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
        customerRepository.save(customer.archived());
    }
}
