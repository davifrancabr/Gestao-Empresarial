package com.empresa.gestao.customer.infrastructure.persistence.adapter;

import com.empresa.gestao.customer.domain.repository.CustomerFilter;
import com.empresa.gestao.customer.application.dto.CustomerInfo;
import com.empresa.gestao.customer.application.port.CustomerQueryPort;
import com.empresa.gestao.customer.domain.model.Customer;
import com.empresa.gestao.customer.domain.repository.CustomerRepository;
import com.empresa.gestao.customer.infrastructure.persistence.entity.CustomerJpaEntity;
import com.empresa.gestao.customer.infrastructure.persistence.mapper.CustomerPersistenceMapper;
import com.empresa.gestao.customer.infrastructure.persistence.repository.CustomerJpaRepository;
import com.empresa.gestao.customer.infrastructure.persistence.repository.CustomerSpecifications;
import com.empresa.gestao.shared.domain.query.PageQuery;
import com.empresa.gestao.shared.domain.query.PageResult;
import com.empresa.gestao.shared.infrastructure.persistence.JpaPageMapper;
import com.empresa.gestao.shared.infrastructure.persistence.PageableFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class CustomerRepositoryAdapter implements CustomerRepository, CustomerQueryPort {

    private final CustomerJpaRepository repository;

    public CustomerRepositoryAdapter(CustomerJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Customer> findByIdAndStoreId(UUID id, UUID storeId) {
        return repository.findByIdAndStoreId(id, storeId).map(CustomerPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Customer> findByIdAndStoreIdAndActiveTrue(UUID id, UUID storeId) {
        return repository.findByIdAndStoreIdAndActiveTrue(id, storeId)
                .map(CustomerPersistenceMapper::toDomain);
    }

    @Override
    public PageResult<Customer> search(CustomerFilter filter, PageQuery pageQuery) {
        return JpaPageMapper.toPageResult(
                repository.findAll(CustomerSpecifications.filtered(filter),
                        PageableFactory.toPageable(pageQuery)),
                CustomerPersistenceMapper::toDomain);
    }

    @Override
    public Customer save(Customer customer) {
        CustomerJpaEntity entity = customer.id() == null
                ? new CustomerJpaEntity()
                : repository.findById(customer.id()).orElseGet(CustomerJpaEntity::new);
        CustomerPersistenceMapper.apply(customer, entity);
        return CustomerPersistenceMapper.toDomain(repository.save(entity));
    }

    @Override
    public List<UUID> findIdsByName(UUID storeId, String term) {
        return repository.findIdsByName(storeId, term);
    }

    @Override
    public Map<UUID, String> findNamesByIds(Collection<UUID> ids) {
        Map<UUID, String> names = new LinkedHashMap<>();
        repository.findAllById(ids).forEach(entity -> names.put(entity.getId(), entity.getName()));
        return names;
    }

    @Override
    public Optional<CustomerInfo> findActive(UUID storeId, UUID id) {
        return repository.findByIdAndStoreIdAndActiveTrue(id, storeId)
                .map(CustomerPersistenceMapper::toDomain)
                .map(customer -> new CustomerInfo(customer.id(), customer.storeId(), customer.name(),
                        customer.active()));
    }
}
