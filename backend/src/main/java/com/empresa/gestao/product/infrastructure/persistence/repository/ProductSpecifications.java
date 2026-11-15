package com.empresa.gestao.product.infrastructure.persistence.repository;

import com.empresa.gestao.product.domain.repository.ProductFilter;
import com.empresa.gestao.product.infrastructure.persistence.entity.ProductJpaEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<ProductJpaEntity> filtered(ProductFilter filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("storeId"), filter.storeId()));
            if (!filter.includeInactive()) {
                predicates.add(builder.isTrue(root.get("active")));
            }
            if (filter.search() != null) {
                predicates.add(builder.or(
                        builder.like(root.get("normalizedName"), filter.search()),
                        builder.like(builder.lower(builder.coalesce(root.get("sku"), "")), filter.search())));
            }
            if (filter.category() != null) {
                predicates.add(builder.equal(root.get("category"), filter.category()));
            }
            if (filter.status() != null) {
                switch (filter.status()) {
                    case NORMAL -> predicates.add(
                            builder.greaterThan(root.get("quantity"), root.get("minimum")));
                    case LOW -> predicates.add(builder.and(
                            builder.greaterThan(root.get("quantity"), 0),
                            builder.lessThanOrEqualTo(root.get("quantity"), root.get("minimum"))));
                    case OUT_OF_STOCK -> predicates.add(builder.equal(root.get("quantity"), 0));
                }
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
