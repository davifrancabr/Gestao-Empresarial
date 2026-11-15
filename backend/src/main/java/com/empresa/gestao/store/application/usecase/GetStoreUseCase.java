package com.empresa.gestao.store.application.usecase;

import com.empresa.gestao.shared.application.dto.AuthenticatedUser;
import com.empresa.gestao.shared.application.port.CurrentUserPort;
import com.empresa.gestao.shared.domain.NotFoundException;
import com.empresa.gestao.store.application.dto.StoreResult;
import com.empresa.gestao.store.domain.model.Store;
import com.empresa.gestao.store.domain.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetStoreUseCase {

    private final StoreRepository storeRepository;
    private final CurrentUserPort currentUser;

    public GetStoreUseCase(StoreRepository storeRepository, CurrentUserPort currentUser) {
        this.storeRepository = storeRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public StoreResult execute(UUID id) {
        AuthenticatedUser user = currentUser.currentUser();
        if (!user.hasStoreAccess(id)) {
            throw new NotFoundException("Loja não encontrada.");
        }
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Loja não encontrada."));
        return StoreResultMapper.toResult(store);
    }
}
