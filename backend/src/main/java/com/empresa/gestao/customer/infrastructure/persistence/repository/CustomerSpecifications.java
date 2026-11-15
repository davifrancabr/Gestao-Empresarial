package com.empresa.gestao.customer.infrastructure.persistence.repository;

import com.empresa.gestao.customer.domain.repository.CustomerFilter;
import com.empresa.gestao.customer.infrastructure.persistence.entity.CustomerJpaEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtros dinâmicos da listagem de clientes.
 */
public final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    public static Specification<CustomerJpaEntity> filtered(CustomerFilter filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("storeId"), filter.storeId()));

            if (!filter.includeInactive()) {
                predicates.add(builder.isTrue(root.get("active")));
            }
            if (filter.searchTerm() != null) {
                predicates.add(builder.or(
                        builder.like(root.get("normalizedName"), filter.searchTerm()),
                        builder.like(builder.lower(builder.coalesce(root.get("email"), "")),
                                filter.searchTerm()),
                        filter.digitsTerm() == null ? builder.disjunction()
                                : builder.like(builder.coalesce(root.get("cpf"), ""), filter.digitsTerm())));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
