package com.empresa.gestao.store.application.usecase;

import com.empresa.gestao.shared.application.dto.AuthenticatedUser;
import com.empresa.gestao.shared.application.port.CurrentUserPort;
import com.empresa.gestao.shared.domain.ForbiddenException;
import com.empresa.gestao.store.domain.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ResolveStoreAccessUseCase {

    private final StoreRepository storeRepository;
    private final CurrentUserPort currentUser;

    public ResolveStoreAccessUseCase(StoreRepository storeRepository, CurrentUserPort currentUser) {
        this.storeRepository = storeRepository;
        this.currentUser = currentUser;
    }

    /**
     * Resolve a loja da requisição. Usuários comuns só acessam lojas vinculadas;
     * administradores podem acessar qualquer loja ativa. Sem parâmetro, usa a
     * primeira loja vinculada ao usuário.
     */
    @Transactional(readOnly = true)
    public UUID execute(UUID requestedStoreId) {
        AuthenticatedUser user = currentUser.currentUser();
        if (requestedStoreId != null) {
            if (!user.hasStoreAccess(requestedStoreId)
                    || !storeRepository.existsByIdAndActiveTrue(requestedStoreId)) {
                throw new ForbiddenException("Você não tem acesso a esta loja.");
            }
            return requestedStoreId;
        }
        if (!user.stores().isEmpty()) {
            return user.stores().getFirst().id();
        }
        if (user.admin()) {
            return storeRepository.findByActiveTrueOrderByName().stream()
                    .findFirst()
                    .map(store -> store.id())
                    .orElseThrow(() -> new ForbiddenException("Nenhuma loja ativa disponível."));
        }
        throw new ForbiddenException("Seu usuário não está vinculado a nenhuma loja.");
    }
}
