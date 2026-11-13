package com.empresa.gestao.sale;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Filtros dinâmicos da listagem de vendas.
 */
public final class SaleSpecifications {

    private SaleSpecifications() {
    }

    public static Specification<Sale> filtered(UUID storeId, UUID customerId, Instant from, Instant to,
                                               String searchTerm) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("store").get("id"), storeId));

            if (customerId != null) {
                predicates.add(builder.equal(root.get("customer").get("id"), customerId));
            }
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(builder.lessThan(root.get("createdAt"), to));
            }
            if (searchTerm != null) {
                var customer = root.join("customer", JoinType.LEFT);
                predicates.add(builder.like(customer.get("normalizedName"), searchTerm));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
