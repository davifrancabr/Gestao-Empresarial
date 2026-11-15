package com.empresa.gestao.auth.application.usecase;

import com.empresa.gestao.auth.application.dto.AuthSessionResult;
import com.empresa.gestao.shared.application.dto.AuthenticatedUser;
import com.empresa.gestao.shared.application.port.CurrentUserPort;
import com.empresa.gestao.store.application.dto.StoreSummary;
import org.springframework.stereotype.Service;

@Service
public class CurrentSessionUseCase {

    private final CurrentUserPort currentUser;

    public CurrentSessionUseCase(CurrentUserPort currentUser) {
        this.currentUser = currentUser;
    }

    public AuthSessionResult execute() {
        AuthenticatedUser user = currentUser.currentUser();
        var summary = new AuthSessionResult.UserSummary(user.id(), user.name(), user.email(), user.role());
        var stores = user.stores().stream()
                .map(store -> new StoreSummary(store.id(), store.name()))
                .toList();
        var activeStoreId = stores.isEmpty() ? null : stores.getFirst().id();
        return new AuthSessionResult(summary, stores, activeStoreId);
    }
}
