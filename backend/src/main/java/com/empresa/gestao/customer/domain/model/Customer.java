package com.empresa.gestao.customer.domain.model;

import java.time.Instant;
import java.util.UUID;

public final class Customer {

    private final UUID id;
    private final UUID storeId;
    private final String name;
    private final String normalizedName;
    private final String cpf;
    private final String phone;
    private final String email;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Customer(UUID id, UUID storeId, String name, String normalizedName, String cpf,
                    String phone, String email, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.storeId = storeId;
        this.name = name;
        this.normalizedName = normalizedName;
        this.cpf = cpf;
        this.phone = phone;
        this.email = email;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Customer archived() {
        return new Customer(id, storeId, name, normalizedName, cpf, phone, email, false,
                createdAt, updatedAt);
    }

    public UUID id() {
        return id;
    }

    public UUID storeId() {
        return storeId;
    }

    public String name() {
        return name;
    }

    public String normalizedName() {
        return normalizedName;
    }

    public String cpf() {
        return cpf;
    }

    public String phone() {
        return phone;
    }

    public String email() {
        return email;
    }

    public boolean active() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
