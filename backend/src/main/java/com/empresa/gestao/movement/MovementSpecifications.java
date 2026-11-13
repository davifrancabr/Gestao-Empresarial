package com.empresa.gestao.movement;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Filtros dinâmicos do histórico de movimentações. Os filtros opcionais são
 * adicionados somente quando informados, evitando parâmetros nulos no SQL.
 */
public final class MovementSpecifications {

    private MovementSpecifications() {
    }

    public static Specification<StockMovement> filtered(UUID storeId, UUID productId, MovementType type,
                                                        Instant from, Instant to, String searchTerm) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("store").get("id"), storeId));

            if (productId != null) {
                predicates.add(builder.equal(root.get("product").get("id"), productId));
            }
            if (type != null) {
                predicates.add(builder.equal(root.get("type"), type));
            }
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(builder.lessThan(root.get("createdAt"), to));
            }
            if (searchTerm != null) {
                var product = root.join("product", JoinType.INNER);
                predicates.add(builder.or(
                        builder.like(product.get("normalizedName"), searchTerm),
                        builder.like(builder.lower(builder.coalesce(product.get("sku"), "")), searchTerm)));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
