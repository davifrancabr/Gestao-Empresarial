package com.empresa.gestao.sale;

import com.empresa.gestao.common.dto.PageResponse;
import com.empresa.gestao.sale.dto.SaleCreateRequest;
import com.empresa.gestao.sale.dto.SaleResponse;
import com.empresa.gestao.sale.dto.SaleSummaryResponse;
import com.empresa.gestao.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/sales")
public class SaleController {

    private final SaleService saleService;
    private final CurrentUserService currentUser;

    public SaleController(SaleService saleService, CurrentUserService currentUser) {
        this.saleService = saleService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<SaleSummaryResponse> list(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return saleService.list(currentUser.resolveStoreId(storeId), customerId, from, to,
                search, page, size, sort);
    }

    @GetMapping("/{id}")
    public SaleResponse get(@PathVariable UUID id,
                            @RequestParam(required = false) UUID storeId) {
        return saleService.get(currentUser.resolveStoreId(storeId), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public SaleResponse create(@Valid @RequestBody SaleCreateRequest request,
                               @RequestParam(required = false) UUID storeId) {
        return saleService.create(currentUser.resolveStoreId(storeId), currentUser.userId(), request);
    }
}
