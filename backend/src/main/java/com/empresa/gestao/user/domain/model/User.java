package com.empresa.gestao.user.domain.model;

import com.empresa.gestao.shared.domain.model.Role;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class User {

    private final UUID id;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean active;
    private final List<UUID> storeIds;
    private final Instant createdAt;
    private final Instant updatedAt;

    public User(UUID id, String name, String email, String passwordHash, Role role, boolean active,
                List<UUID> storeIds, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.storeIds = List.copyOf(storeIds);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Role role() {
        return role;
    }

    public boolean active() {
        return active;
    }

    public List<UUID> storeIds() {
        return storeIds;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
