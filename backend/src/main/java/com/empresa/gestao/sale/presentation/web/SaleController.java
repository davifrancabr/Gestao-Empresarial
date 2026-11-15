package com.empresa.gestao.sale.presentation.web;

import com.empresa.gestao.sale.application.dto.SaleCreateCommand;
import com.empresa.gestao.sale.application.dto.SaleItemCommand;
import com.empresa.gestao.sale.application.dto.SaleResult;
import com.empresa.gestao.sale.application.dto.SaleSummaryResult;
import com.empresa.gestao.sale.application.usecase.CreateSaleUseCase;
import com.empresa.gestao.sale.application.usecase.GetSaleUseCase;
import com.empresa.gestao.sale.application.usecase.ListSalesUseCase;
import com.empresa.gestao.sale.presentation.web.request.SaleCreateRequest;
import com.empresa.gestao.shared.application.port.CurrentUserPort;
import com.empresa.gestao.shared.presentation.web.PageResponse;
import com.empresa.gestao.store.application.usecase.ResolveStoreAccessUseCase;
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

    private final ListSalesUseCase listSales;
    private final GetSaleUseCase getSale;
    private final CreateSaleUseCase createSale;
    private final ResolveStoreAccessUseCase resolveStore;
    private final CurrentUserPort currentUser;

    public SaleController(ListSalesUseCase listSales, GetSaleUseCase getSale,
                          CreateSaleUseCase createSale, ResolveStoreAccessUseCase resolveStore,
                          CurrentUserPort currentUser) {
        this.listSales = listSales;
        this.getSale = getSale;
        this.createSale = createSale;
        this.resolveStore = resolveStore;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<SaleSummaryResult> list(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PageResponse.from(listSales.execute(resolveStore.execute(storeId), customerId,
                from, to, search, page, size, sort));
    }

    @GetMapping("/{id}")
    public SaleResult get(@PathVariable UUID id,
                          @RequestParam(required = false) UUID storeId) {
        return getSale.execute(resolveStore.execute(storeId), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public SaleResult create(@Valid @RequestBody SaleCreateRequest request,
                             @RequestParam(required = false) UUID storeId) {
        UUID store = resolveStore.execute(storeId);
        var items = request.items().stream()
                .map(item -> new SaleItemCommand(item.productId(), item.quantity(), item.unitPrice()))
                .toList();
        return createSale.execute(store, currentUser.currentUser().id(),
                new SaleCreateCommand(request.customerId(), items));
    }
}
