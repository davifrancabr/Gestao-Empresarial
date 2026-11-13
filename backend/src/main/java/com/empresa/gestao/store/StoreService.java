package com.empresa.gestao.store;

import com.empresa.gestao.common.exception.ConflictException;
import com.empresa.gestao.common.exception.ResourceNotFoundException;
import com.empresa.gestao.common.util.TextNormalizer;
import com.empresa.gestao.security.AppUserPrincipal;
import com.empresa.gestao.security.CurrentUserService;
import com.empresa.gestao.store.dto.StoreRequest;
import com.empresa.gestao.store.dto.StoreResponse;
import com.empresa.gestao.store.dto.StoreSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class StoreService {

    private final StoreRepository storeRepository;
    private final CurrentUserService currentUser;

    public StoreService(StoreRepository storeRepository, CurrentUserService currentUser) {
        this.storeRepository = storeRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<StoreResponse> listAccessible() {
        AppUserPrincipal principal = currentUser.principal();
        if (principal.isAdmin()) {
            return storeRepository.findByActiveTrueOrderByName().stream().map(this::toResponse).toList();
        }
        List<UUID> ids = principal.storeIds();
        return ids.stream()
                .map(storeRepository::findById)
                .flatMap(java.util.Optional::stream)
                .filter(Store::isActive)
                .sorted(Comparator.comparing(Store::getName))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoreResponse get(UUID id) {
        AppUserPrincipal principal = currentUser.principal();
        if (!principal.hasStoreAccess(id)) {
            throw new ResourceNotFoundException("Loja não encontrada.");
        }
        return toResponse(findStore(id));
    }

    @Transactional
    public StoreResponse create(StoreRequest request) {
        Store store = new Store();
        apply(store, request);
        return toResponse(storeRepository.save(store));
    }

    @Transactional
    public StoreResponse update(UUID id, StoreRequest request) {
        Store store = findStore(id);
        apply(store, request);
        return toResponse(storeRepository.save(store));
    }

    @Transactional(readOnly = true)
    public Store findStore(UUID id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loja não encontrada."));
    }

    public List<StoreSummary> accessibleSummaries() {
        return listAccessible().stream().map(store -> new StoreSummary(store.id(), store.name())).toList();
    }

    private void apply(Store store, StoreRequest request) {
        String name = TextNormalizer.cleanText(request.name());
        if (name == null) {
            throw new ConflictException("Informe o nome da loja.");
        }
        store.setName(name);
        store.setDocument(TextNormalizer.cleanText(request.document()));
        if (request.active() != null) {
            store.setActive(request.active());
        }
    }

    private StoreResponse toResponse(Store store) {
        return new StoreResponse(store.getId(), store.getName(), store.getDocument(), store.isActive(),
                store.getCreatedAt(), store.getUpdatedAt());
    }
}
