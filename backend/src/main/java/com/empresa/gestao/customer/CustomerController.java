package com.empresa.gestao.customer;

import com.empresa.gestao.common.dto.PageResponse;
import com.empresa.gestao.customer.dto.CustomerRequest;
import com.empresa.gestao.customer.dto.CustomerResponse;
import com.empresa.gestao.sale.dto.SaleSummaryResponse;
import com.empresa.gestao.security.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final CurrentUserService currentUser;

    public CustomerController(CustomerService customerService, CurrentUserService currentUser) {
        this.customerService = customerService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<CustomerResponse> list(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return customerService.list(currentUser.resolveStoreId(storeId), search, includeInactive,
                page, size, sort);
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable UUID id,
                                @RequestParam(required = false) UUID storeId) {
        return customerService.get(currentUser.resolveStoreId(storeId), id);
    }

    @GetMapping("/{id}/purchases")
    public PageResponse<SaleSummaryResponse> purchases(@PathVariable UUID id,
                                                       @RequestParam(required = false) UUID storeId,
                                                       @RequestParam(required = false) Integer page,
                                                       @RequestParam(required = false) Integer size) {
        return customerService.purchases(currentUser.resolveStoreId(storeId), id, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public CustomerResponse create(@Valid @RequestBody CustomerRequest request,
                                   @RequestParam(required = false) UUID storeId) {
        return customerService.create(currentUser.resolveStoreId(storeId), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody CustomerRequest request,
                                   @RequestParam(required = false) UUID storeId) {
        return customerService.update(currentUser.resolveStoreId(storeId), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public void archive(@PathVariable UUID id, @RequestParam(required = false) UUID storeId) {
        customerService.archive(currentUser.resolveStoreId(storeId), id);
    }
}
