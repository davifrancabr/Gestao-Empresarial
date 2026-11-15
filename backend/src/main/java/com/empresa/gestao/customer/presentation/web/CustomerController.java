package com.empresa.gestao.customer.presentation.web;

import com.empresa.gestao.customer.application.dto.CustomerCommand;
import com.empresa.gestao.customer.application.dto.CustomerPurchaseResult;
import com.empresa.gestao.customer.application.dto.CustomerResult;
import com.empresa.gestao.customer.application.usecase.ArchiveCustomerUseCase;
import com.empresa.gestao.customer.application.usecase.CreateCustomerUseCase;
import com.empresa.gestao.customer.application.usecase.GetCustomerUseCase;
import com.empresa.gestao.customer.application.usecase.ListCustomerPurchasesUseCase;
import com.empresa.gestao.customer.application.usecase.ListCustomersUseCase;
import com.empresa.gestao.customer.application.usecase.UpdateCustomerUseCase;
import com.empresa.gestao.customer.presentation.web.request.CustomerRequest;
import com.empresa.gestao.shared.presentation.web.PageResponse;
import com.empresa.gestao.store.application.usecase.ResolveStoreAccessUseCase;
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

    private final ListCustomersUseCase listCustomers;
    private final GetCustomerUseCase getCustomer;
    private final ListCustomerPurchasesUseCase listPurchases;
    private final CreateCustomerUseCase createCustomer;
    private final UpdateCustomerUseCase updateCustomer;
    private final ArchiveCustomerUseCase archiveCustomer;
    private final ResolveStoreAccessUseCase resolveStore;

    public CustomerController(ListCustomersUseCase listCustomers, GetCustomerUseCase getCustomer,
                              ListCustomerPurchasesUseCase listPurchases,
                              CreateCustomerUseCase createCustomer,
                              UpdateCustomerUseCase updateCustomer,
                              ArchiveCustomerUseCase archiveCustomer,
                              ResolveStoreAccessUseCase resolveStore) {
        this.listCustomers = listCustomers;
        this.getCustomer = getCustomer;
        this.listPurchases = listPurchases;
        this.createCustomer = createCustomer;
        this.updateCustomer = updateCustomer;
        this.archiveCustomer = archiveCustomer;
        this.resolveStore = resolveStore;
    }

    @GetMapping
    public PageResponse<CustomerResult> list(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PageResponse.from(listCustomers.execute(resolveStore.execute(storeId), search,
                includeInactive, page, size, sort));
    }

    @GetMapping("/{id}")
    public CustomerResult get(@PathVariable UUID id,
                              @RequestParam(required = false) UUID storeId) {
        return getCustomer.execute(resolveStore.execute(storeId), id);
    }

    @GetMapping("/{id}/purchases")
    public PageResponse<CustomerPurchaseResult> purchases(@PathVariable UUID id,
                                                          @RequestParam(required = false) UUID storeId,
                                                          @RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size) {
        return PageResponse.from(listPurchases.execute(resolveStore.execute(storeId), id, page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public CustomerResult create(@Valid @RequestBody CustomerRequest request,
                                 @RequestParam(required = false) UUID storeId) {
        return createCustomer.execute(resolveStore.execute(storeId), toCommand(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public CustomerResult update(@PathVariable UUID id, @Valid @RequestBody CustomerRequest request,
                                 @RequestParam(required = false) UUID storeId) {
        return updateCustomer.execute(resolveStore.execute(storeId), id, toCommand(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public void archive(@PathVariable UUID id, @RequestParam(required = false) UUID storeId) {
        archiveCustomer.execute(resolveStore.execute(storeId), id);
    }

    private CustomerCommand toCommand(CustomerRequest request) {
        return new CustomerCommand(request.name(), request.cpf(), request.phone(),
                request.email(), request.active());
    }
}
