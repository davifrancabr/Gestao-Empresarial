package com.empresa.gestao.product;

import com.empresa.gestao.product.dto.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getStore().getId(),
                product.getName(),
                product.getSku(),
                product.getCategory(),
                product.getDescription(),
                product.getQuantity(),
                product.getMinimum(),
                product.getUnit(),
                product.status(),
                product.isActive(),
                product.getVersion(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
