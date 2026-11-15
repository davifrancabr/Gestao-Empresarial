package com.empresa.gestao.customer.infrastructure.persistence.mapper;

import com.empresa.gestao.customer.domain.model.Customer;
import com.empresa.gestao.customer.infrastructure.persistence.entity.CustomerJpaEntity;

public final class CustomerPersistenceMapper {

    private CustomerPersistenceMapper() {
    }

    public static Customer toDomain(CustomerJpaEntity entity) {
        return new Customer(entity.getId(), entity.getStoreId(), entity.getName(),
                entity.getNormalizedName(), entity.getCpf(), entity.getPhone(), entity.getEmail(),
                entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static void apply(Customer customer, CustomerJpaEntity entity) {
        entity.setStoreId(customer.storeId());
        entity.setName(customer.name());
        entity.setNormalizedName(customer.normalizedName());
        entity.setCpf(customer.cpf());
        entity.setPhone(customer.phone());
        entity.setEmail(customer.email());
        entity.setActive(customer.active());
    }
}
