package com.empresa.gestao.movement.presentation.web;

import com.empresa.gestao.movement.application.dto.MovementCommand;
import com.empresa.gestao.movement.application.dto.MovementResult;
import com.empresa.gestao.movement.application.usecase.GetMovementUseCase;
import com.empresa.gestao.movement.application.usecase.RecordMovementUseCase;
import com.empresa.gestao.movement.application.usecase.SearchMovementsUseCase;
import com.empresa.gestao.movement.presentation.web.request.MovementRequest;
import com.empresa.gestao.shared.application.port.CurrentUserPort;
import com.empresa.gestao.shared.domain.model.MovementType;
import com.empresa.gestao.shared.presentation.web.PageResponse;
import com.empresa.gestao.store.application.usecase.ResolveStoreAccessUseCase;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock/movements")
public class MovementController {

    private final SearchMovementsUseCase searchMovements;
    private final GetMovementUseCase getMovement;
    private final RecordMovementUseCase recordMovement;
    private final ResolveStoreAccessUseCase resolveStore;
    private final CurrentUserPort currentUser;

    public MovementController(SearchMovementsUseCase searchMovements, GetMovementUseCase getMovement,
                              RecordMovementUseCase recordMovement, ResolveStoreAccessUseCase resolveStore,
                              CurrentUserPort currentUser) {
        this.searchMovements = searchMovements;
        this.getMovement = getMovement;
        this.recordMovement = recordMovement;
        this.resolveStore = resolveStore;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<MovementResult> list(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        UUID store = resolveStore.execute(storeId);
        return PageResponse.from(searchMovements.execute(store, productId, type, from, to, search,
                page, size, sort));
    }

    @GetMapping("/{id}")
    public MovementResult get(@PathVariable UUID id,
                              @RequestParam(required = false) UUID storeId) {
        return getMovement.execute(resolveStore.execute(storeId), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResult create(@Valid @RequestBody MovementRequest request,
                                 @RequestParam(required = false) UUID storeId) {
        UUID store = resolveStore.execute(storeId);
        var user = currentUser.currentUser();
        return recordMovement.record(store, user.id(), user.canManage(), new MovementCommand(
                request.productId(), request.type(), request.quantity(), request.reason(),
                request.observation()));
    }
}
