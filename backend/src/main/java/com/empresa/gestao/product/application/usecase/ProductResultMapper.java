package com.empresa.gestao.product.application.usecase;

import com.empresa.gestao.product.application.dto.ProductInfo;
import com.empresa.gestao.product.application.dto.ProductResult;
import com.empresa.gestao.product.domain.model.Product;

final class ProductResultMapper {

    private ProductResultMapper() {
    }

    static ProductResult toResult(Product product) {
        return new ProductResult(product.id(), product.storeId(), product.name(), product.sku(),
                product.category(), product.description(), product.quantity(), product.minimum(),
                product.unit(), product.status(), product.active(), product.version(),
                product.createdAt(), product.updatedAt());
    }

    static ProductInfo toInfo(Product product) {
        return new ProductInfo(product.id(), product.storeId(), product.name(), product.sku(),
                product.quantity(), product.minimum(), product.unit(), product.active(),
                product.version());
    }
}
