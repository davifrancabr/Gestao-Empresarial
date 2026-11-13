package com.empresa.gestao.product;

import com.empresa.gestao.product.dto.ProductCreateRequest;
import com.empresa.gestao.product.dto.ProductResponse;
import com.empresa.gestao.product.dto.ProductUpdateRequest;
import com.empresa.gestao.security.AppUserPrincipal;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;
    private final CurrentUserService currentUser;

    public ProductController(ProductService productService, CurrentUserService currentUser) {
        this.productService = productService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public com.empresa.gestao.common.dto.PageResponse<ProductResponse> list(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        UUID store = currentUser.resolveStoreId(storeId);
        return productService.list(store, search, category, status, sort, page, size, includeInactive);
    }

    @GetMapping("/categories")
    public List<String> categories(@RequestParam(required = false) UUID storeId) {
        return productService.categories(currentUser.resolveStoreId(storeId));
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable UUID id, @RequestParam(required = false) UUID storeId) {
        return productService.get(currentUser.resolveStoreId(storeId), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ProductResponse create(@Valid @RequestBody ProductCreateRequest request,
                                  @RequestParam(required = false) UUID storeId) {
        UUID store = currentUser.resolveStoreId(storeId);
        return productService.create(store, currentUser.userId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductUpdateRequest request,
                                  @RequestParam(required = false) UUID storeId) {
        return productService.update(currentUser.resolveStoreId(storeId), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public void archive(@PathVariable UUID id, @RequestParam(required = false) UUID storeId) {
        productService.archive(currentUser.resolveStoreId(storeId), id);
    }
}
