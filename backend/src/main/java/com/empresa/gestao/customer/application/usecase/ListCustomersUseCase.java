package com.empresa.gestao.customer.application.usecase;

import com.empresa.gestao.customer.domain.repository.CustomerFilter;
import com.empresa.gestao.customer.application.dto.CustomerResult;
import com.empresa.gestao.customer.domain.repository.CustomerRepository;
import com.empresa.gestao.shared.domain.query.PageQuery;
import com.empresa.gestao.shared.domain.query.PageResult;
import com.empresa.gestao.shared.domain.util.TextNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class ListCustomersUseCase {

    private final CustomerRepository customerRepository;

    public ListCustomersUseCase(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<CustomerResult> execute(UUID storeId, String search, boolean includeInactive,
                                              Integer page, Integer size, String sort) {
        String term = TextNormalizer.containsPattern(search);
        String digits = TextNormalizer.onlyDigits(search);
        String digitsTerm = digits == null ? null : "%" + digits + "%";
        PageQuery query = PageQuery.of(page, size, sort, Map.of(
                "name", "name",
                "createdAt", "createdAt"
        ), "name,asc");
        CustomerFilter filter = new CustomerFilter(storeId, includeInactive, term, digitsTerm);
        return customerRepository.search(filter, query).map(CustomerResultMapper::toResult);
    }
}
