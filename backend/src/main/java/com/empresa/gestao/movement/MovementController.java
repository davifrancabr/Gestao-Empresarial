package com.empresa.gestao.movement;

import com.empresa.gestao.common.dto.PageResponse;
import com.empresa.gestao.movement.dto.MovementRequest;
import com.empresa.gestao.movement.dto.MovementResponse;
import com.empresa.gestao.security.CurrentUserService;
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

    private final MovementService movementService;
    private final CurrentUserService currentUser;

    public MovementController(MovementService movementService, CurrentUserService currentUser) {
        this.movementService = movementService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<MovementResponse> list(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        UUID store = currentUser.resolveStoreId(storeId);
        return movementService.search(store, productId, type, from, to, search, page, size, sort);
    }

    @GetMapping("/{id}")
    public MovementResponse get(@PathVariable UUID id,
                                @RequestParam(required = false) UUID storeId) {
        return movementService.get(currentUser.resolveStoreId(storeId), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovementResponse create(@Valid @RequestBody MovementRequest request,
                                   @RequestParam(required = false) UUID storeId) {
        UUID store = currentUser.resolveStoreId(storeId);
        boolean allowAdjustment = currentUser.principal().canManage();
        return movementService.record(store, currentUser.userId(), allowAdjustment, request);
    }
}
