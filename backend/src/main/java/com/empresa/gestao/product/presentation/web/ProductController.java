package com.empresa.gestao.product.presentation.web;

import com.empresa.gestao.product.application.dto.ProductCreateCommand;
import com.empresa.gestao.product.application.dto.ProductResult;
import com.empresa.gestao.product.application.dto.ProductUpdateCommand;
import com.empresa.gestao.product.application.usecase.ArchiveProductUseCase;
import com.empresa.gestao.product.application.usecase.CreateProductUseCase;
import com.empresa.gestao.product.application.usecase.GetProductUseCase;
import com.empresa.gestao.product.application.usecase.ListCategoriesUseCase;
import com.empresa.gestao.product.application.usecase.ListProductsUseCase;
import com.empresa.gestao.product.application.usecase.UpdateProductUseCase;
import com.empresa.gestao.product.presentation.web.request.ProductCreateRequest;
import com.empresa.gestao.product.presentation.web.request.ProductUpdateRequest;
import com.empresa.gestao.shared.application.port.CurrentUserPort;
import com.empresa.gestao.shared.domain.model.ProductStatus;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ListProductsUseCase listProducts;
    private final ListCategoriesUseCase listCategories;
    private final GetProductUseCase getProduct;
    private final CreateProductUseCase createProduct;
    private final UpdateProductUseCase updateProduct;
    private final ArchiveProductUseCase archiveProduct;
    private final ResolveStoreAccessUseCase resolveStore;
    private final CurrentUserPort currentUser;

    public ProductController(ListProductsUseCase listProducts, ListCategoriesUseCase listCategories,
                             GetProductUseCase getProduct, CreateProductUseCase createProduct,
                             UpdateProductUseCase updateProduct, ArchiveProductUseCase archiveProduct,
                             ResolveStoreAccessUseCase resolveStore, CurrentUserPort currentUser) {
        this.listProducts = listProducts;
        this.listCategories = listCategories;
        this.getProduct = getProduct;
        this.createProduct = createProduct;
        this.updateProduct = updateProduct;
        this.archiveProduct = archiveProduct;
        this.resolveStore = resolveStore;
        this.currentUser = currentUser;
    }

    @GetMapping
    public PageResponse<ProductResult> list(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        UUID store = resolveStore.execute(storeId);
        return PageResponse.from(listProducts.execute(store, search, category, status, sort,
                page, size, includeInactive));
    }

    @GetMapping("/categories")
    public List<String> categories(@RequestParam(required = false) UUID storeId) {
        return listCategories.execute(resolveStore.execute(storeId));
    }

    @GetMapping("/{id}")
    public ProductResult get(@PathVariable UUID id, @RequestParam(required = false) UUID storeId) {
        return getProduct.execute(resolveStore.execute(storeId), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ProductResult create(@Valid @RequestBody ProductCreateRequest request,
                                @RequestParam(required = false) UUID storeId) {
        UUID store = resolveStore.execute(storeId);
        return createProduct.execute(store, currentUser.currentUser().id(),
                new ProductCreateCommand(request.name(), request.sku(), request.category(),
                        request.description(), request.quantity(), request.minimum(), request.unit()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public ProductResult update(@PathVariable UUID id, @Valid @RequestBody ProductUpdateRequest request,
                                @RequestParam(required = false) UUID storeId) {
        return updateProduct.execute(resolveStore.execute(storeId), id,
                new ProductUpdateCommand(request.name(), request.sku(), request.category(),
                        request.description(), request.minimum(), request.unit(), request.version()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public void archive(@PathVariable UUID id, @RequestParam(required = false) UUID storeId) {
        archiveProduct.execute(resolveStore.execute(storeId), id);
    }
}
