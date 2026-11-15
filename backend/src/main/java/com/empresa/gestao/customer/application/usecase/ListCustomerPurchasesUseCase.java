package com.empresa.gestao.customer.application.usecase;

import com.empresa.gestao.customer.application.dto.CustomerPurchaseResult;
import com.empresa.gestao.customer.application.port.CustomerPurchasePort;
import com.empresa.gestao.customer.domain.repository.CustomerRepository;
import com.empresa.gestao.shared.domain.query.PageResult;
import com.empresa.gestao.shared.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ListCustomerPurchasesUseCase {

    private final CustomerRepository customerRepository;
    private final CustomerPurchasePort customerPurchasePort;

    public ListCustomerPurchasesUseCase(CustomerRepository customerRepository,
                                        CustomerPurchasePort customerPurchasePort) {
        this.customerRepository = customerRepository;
        this.customerPurchasePort = customerPurchasePort;
    }

    @Transactional(readOnly = true)
    public PageResult<CustomerPurchaseResult> execute(UUID storeId, UUID customerId,
                                                      Integer page, Integer size) {
        customerRepository.findByIdAndStoreId(customerId, storeId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
        return customerPurchasePort.findByCustomer(storeId, customerId, page, size);
    }
}
