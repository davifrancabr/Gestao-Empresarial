package com.empresa.gestao.security;

import com.empresa.gestao.common.exception.ForbiddenException;
import com.empresa.gestao.common.exception.UnauthorizedException;
import com.empresa.gestao.store.StoreRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserService {

    private final StoreRepository storeRepository;

    public CurrentUserService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public AppUserPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            throw new UnauthorizedException("Sessão expirada. Faça login novamente.");
        }
        return principal;
    }

    public UUID userId() {
        return principal().id();
    }

    /**
     * Resolve a loja da requisição. Usuários comuns só acessam lojas vinculadas;
     * administradores podem acessar qualquer loja ativa. Sem parâmetro, usa a
     * primeira loja vinculada ao usuário.
     */
    public UUID resolveStoreId(UUID requestedStoreId) {
        AppUserPrincipal principal = principal();
        if (requestedStoreId != null) {
            if (!principal.hasStoreAccess(requestedStoreId) || !storeRepository.existsByIdAndActiveTrue(requestedStoreId)) {
                throw new ForbiddenException("Você não tem acesso a esta loja.");
            }
            return requestedStoreId;
        }
        if (!principal.stores().isEmpty()) {
            return principal.stores().getFirst().id();
        }
        if (principal.isAdmin()) {
            return storeRepository.findByActiveTrueOrderByName().stream()
                    .findFirst()
                    .map(store -> store.getId())
                    .orElseThrow(() -> new ForbiddenException("Nenhuma loja ativa disponível."));
        }
        throw new ForbiddenException("Seu usuário não está vinculado a nenhuma loja.");
    }
}
