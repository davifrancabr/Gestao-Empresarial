package com.empresa.gestao.store.infrastructure.persistence.entity;

import com.empresa.gestao.shared.infrastructure.persistence.AuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "store")
public class StoreJpaEntity extends AuditableJpaEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 20)
    private String document;

    @Column(nullable = false)
    private boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
