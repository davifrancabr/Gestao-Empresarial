package com.empresa.gestao.customer.domain.repository;

import com.empresa.gestao.customer.domain.repository.CustomerFilter;
import com.empresa.gestao.customer.domain.model.Customer;
import com.empresa.gestao.shared.domain.query.PageQuery;
import com.empresa.gestao.shared.domain.query.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {

    Optional<Customer> findByIdAndStoreId(UUID id, UUID storeId);

    Optional<Customer> findByIdAndStoreIdAndActiveTrue(UUID id, UUID storeId);

    PageResult<Customer> search(CustomerFilter filter, PageQuery pageQuery);

    Customer save(Customer customer);

    List<UUID> findIdsByName(UUID storeId, String term);

    Map<UUID, String> findNamesByIds(Collection<UUID> ids);
}
