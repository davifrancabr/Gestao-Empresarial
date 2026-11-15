package com.empresa.gestao.store.application.usecase;

import com.empresa.gestao.shared.application.dto.AuthenticatedUser;
import com.empresa.gestao.shared.application.port.CurrentUserPort;
import com.empresa.gestao.store.application.dto.StoreResult;
import com.empresa.gestao.store.domain.model.Store;
import com.empresa.gestao.store.domain.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class ListAccessibleStoresUseCase {

    private final StoreRepository storeRepository;
    private final CurrentUserPort currentUser;

    public ListAccessibleStoresUseCase(StoreRepository storeRepository, CurrentUserPort currentUser) {
        this.storeRepository = storeRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<StoreResult> execute() {
        AuthenticatedUser user = currentUser.currentUser();
        if (user.admin()) {
            return storeRepository.findByActiveTrueOrderByName().stream()
                    .map(StoreResultMapper::toResult)
                    .toList();
        }
        return user.storeIds().stream()
                .map(storeRepository::findById)
                .flatMap(java.util.Optional::stream)
                .filter(Store::active)
                .sorted(Comparator.comparing(Store::name))
                .map(StoreResultMapper::toResult)
                .toList();
    }
}
