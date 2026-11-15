package com.empresa.gestao.product.infrastructure.persistence.mapper;

import com.empresa.gestao.product.domain.model.Product;
import com.empresa.gestao.product.infrastructure.persistence.entity.ProductJpaEntity;

public final class ProductPersistenceMapper {

    private ProductPersistenceMapper() {
    }

    public static Product toDomain(ProductJpaEntity entity) {
        return new Product(entity.getId(), entity.getStoreId(), entity.getName(),
                entity.getNormalizedName(), entity.getSku(), entity.getCategory(),
                entity.getDescription(), entity.getQuantity(), entity.getMinimum(),
                entity.getUnit(), entity.isActive(), entity.getVersion(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static void apply(Product product, ProductJpaEntity entity) {
        entity.setStoreId(product.storeId());
        entity.setName(product.name());
        entity.setNormalizedName(product.normalizedName());
        entity.setSku(product.sku());
        entity.setCategory(product.category());
        entity.setDescription(product.description());
        entity.setQuantity(product.quantity());
        entity.setMinimum(product.minimum());
        entity.setUnit(product.unit());
        entity.setActive(product.active());
    }
}
