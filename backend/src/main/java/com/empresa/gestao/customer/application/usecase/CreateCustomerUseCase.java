package com.empresa.gestao.customer.application.usecase;

import com.empresa.gestao.customer.application.dto.CustomerCommand;
import com.empresa.gestao.customer.application.dto.CustomerResult;
import com.empresa.gestao.customer.domain.model.Customer;
import com.empresa.gestao.customer.domain.repository.CustomerRepository;
import com.empresa.gestao.shared.domain.BusinessRuleException;
import com.empresa.gestao.shared.domain.util.CpfValidator;
import com.empresa.gestao.shared.domain.util.TextNormalizer;
import com.empresa.gestao.store.application.port.StoreQueryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CreateCustomerUseCase {

    private final CustomerRepository customerRepository;
    private final StoreQueryPort storeQueryPort;

    public CreateCustomerUseCase(CustomerRepository customerRepository, StoreQueryPort storeQueryPort) {
        this.customerRepository = customerRepository;
        this.storeQueryPort = storeQueryPort;
    }

    @Transactional
    public CustomerResult execute(UUID storeId, CustomerCommand command) {
        storeQueryPort.getRequired(storeId);
        String name = cleanName(command.name());
        String cpf = cleanCpf(command.cpf());
        Customer customer = new Customer(null, storeId, name, TextNormalizer.normalizeName(name), cpf,
                TextNormalizer.cleanText(command.phone()), TextNormalizer.cleanText(command.email()),
                command.active() == null || command.active(), null, null);
        return CustomerResultMapper.toResult(customerRepository.save(customer));
    }

    private String cleanName(String value) {
        String name = TextNormalizer.cleanText(value);
        if (name == null) {
            throw new BusinessRuleException("Informe o nome do cliente.");
        }
        return name;
    }

    private String cleanCpf(String value) {
        String cpf = TextNormalizer.onlyDigits(value);
        if (cpf != null && !CpfValidator.isValid(cpf)) {
            throw new BusinessRuleException("Informe um CPF válido ou deixe o campo vazio.");
        }
        return cpf;
    }
}
