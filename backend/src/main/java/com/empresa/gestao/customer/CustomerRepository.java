package com.empresa.gestao.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByIdAndStoreId(UUID id, UUID storeId);

    Optional<Customer> findByIdAndStoreIdAndActiveTrue(UUID id, UUID storeId);
}
