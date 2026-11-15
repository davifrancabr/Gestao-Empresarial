package com.empresa.gestao.security;

import com.empresa.gestao.shared.application.dto.AuthenticatedUser;
import com.empresa.gestao.shared.application.dto.UserStoreRef;
import com.empresa.gestao.shared.application.port.CurrentUserPort;
import com.empresa.gestao.shared.domain.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CurrentUserAdapter implements CurrentUserPort {

    @Override
    public AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new UnauthorizedException("Sessão expirada. Faça login novamente.");
        }
        List<UserStoreRef> stores = principal.stores().stream()
                .map(store -> new UserStoreRef(store.id(), store.name()))
                .toList();
        return new AuthenticatedUser(principal.id(), principal.name(), principal.email(), principal.role(),
                principal.isAdmin(), principal.canManage(), stores);
    }
}
