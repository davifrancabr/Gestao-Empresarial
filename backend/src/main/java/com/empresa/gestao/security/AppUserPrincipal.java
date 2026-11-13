package com.empresa.gestao.security;

import com.empresa.gestao.store.dto.StoreSummary;
import com.empresa.gestao.user.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class AppUserPrincipal implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean active;
    private final List<StoreSummary> stores;

    public AppUserPrincipal(UUID id, String name, String email, String passwordHash, Role role,
                            boolean active, List<StoreSummary> stores) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.stores = List.copyOf(stores);
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

    public Role role() {
        return role;
    }

    public List<StoreSummary> stores() {
        return stores;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean canManage() {
        return role == Role.ADMIN || role == Role.GERENTE;
    }

    public List<UUID> storeIds() {
        return stores.stream().map(StoreSummary::id).toList();
    }

    public boolean hasStoreAccess(UUID storeId) {
        return isAdmin() || storeIds().contains(storeId);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
