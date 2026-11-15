package com.empresa.gestao.customer.application.usecase;

import com.empresa.gestao.customer.application.dto.CustomerCommand;
import com.empresa.gestao.customer.application.dto.CustomerResult;
import com.empresa.gestao.customer.domain.model.Customer;
import com.empresa.gestao.customer.domain.repository.CustomerRepository;
import com.empresa.gestao.shared.domain.BusinessRuleException;
import com.empresa.gestao.shared.domain.NotFoundException;
import com.empresa.gestao.shared.domain.util.CpfValidator;
import com.empresa.gestao.shared.domain.util.TextNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UpdateCustomerUseCase {

    private final CustomerRepository customerRepository;

    public UpdateCustomerUseCase(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResult execute(UUID storeId, UUID id, CustomerCommand command) {
        Customer current = customerRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
        String name = cleanName(command.name());
        Customer customer = new Customer(current.id(), current.storeId(), name,
                TextNormalizer.normalizeName(name), cleanCpf(command.cpf()),
                TextNormalizer.cleanText(command.phone()), TextNormalizer.cleanText(command.email()),
                command.active() == null ? current.active() : command.active(),
                current.createdAt(), current.updatedAt());
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
