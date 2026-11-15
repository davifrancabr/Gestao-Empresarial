package com.empresa.gestao.store.domain.model;

import java.time.Instant;
import java.util.UUID;

public final class Store {

    private final UUID id;
    private final String name;
    private final String document;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Store(UUID id, String name, String document, boolean active,
                 Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.document = document;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String document() {
        return document;
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
