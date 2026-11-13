package com.empresa.gestao.customer;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Filtros dinâmicos da listagem de clientes.
 */
public final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    public static Specification<Customer> filtered(UUID storeId, boolean includeInactive,
                                                   String searchTerm, String digitsTerm) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("store").get("id"), storeId));

            if (!includeInactive) {
                predicates.add(builder.isTrue(root.get("active")));
            }
            if (searchTerm != null) {
                predicates.add(builder.or(
                        builder.like(root.get("normalizedName"), searchTerm),
                        builder.like(builder.lower(builder.coalesce(root.get("email"), "")), searchTerm),
                        digitsTerm == null ? builder.disjunction()
                                : builder.like(builder.coalesce(root.get("cpf"), ""), digitsTerm)));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
