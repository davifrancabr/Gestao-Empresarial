package com.empresa.gestao.store.presentation.web;

import com.empresa.gestao.store.application.dto.StoreCommand;
import com.empresa.gestao.store.application.dto.StoreResult;
import com.empresa.gestao.store.application.usecase.CreateStoreUseCase;
import com.empresa.gestao.store.application.usecase.GetStoreUseCase;
import com.empresa.gestao.store.application.usecase.ListAccessibleStoresUseCase;
import com.empresa.gestao.store.application.usecase.UpdateStoreUseCase;
import com.empresa.gestao.store.presentation.web.request.StoreRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final ListAccessibleStoresUseCase listAccessibleStores;
    private final GetStoreUseCase getStore;
    private final CreateStoreUseCase createStore;
    private final UpdateStoreUseCase updateStore;

    public StoreController(ListAccessibleStoresUseCase listAccessibleStores, GetStoreUseCase getStore,
                           CreateStoreUseCase createStore, UpdateStoreUseCase updateStore) {
        this.listAccessibleStores = listAccessibleStores;
        this.getStore = getStore;
        this.createStore = createStore;
        this.updateStore = updateStore;
    }

    @GetMapping
    public List<StoreResult> list() {
        return listAccessibleStores.execute();
    }

    @GetMapping("/{id}")
    public StoreResult get(@PathVariable UUID id) {
        return getStore.execute(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public StoreResult create(@Valid @RequestBody StoreRequest request) {
        return createStore.execute(new StoreCommand(request.name(), request.document(), request.active()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public StoreResult update(@PathVariable UUID id, @Valid @RequestBody StoreRequest request) {
        return updateStore.execute(id, new StoreCommand(request.name(), request.document(), request.active()));
    }
}
