package com.empresa.gestao.sale.infrastructure.persistence.repository;

import com.empresa.gestao.sale.domain.repository.SaleFilter;
import com.empresa.gestao.sale.infrastructure.persistence.entity.SaleJpaEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Filtros dinâmicos da listagem de vendas.
 */
public final class SaleSpecifications {

    private SaleSpecifications() {
    }

    public static Specification<SaleJpaEntity> filtered(SaleFilter filter,
                                                        Collection<UUID> customerIds) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("storeId"), filter.storeId()));

            if (filter.customerId() != null) {
                predicates.add(builder.equal(root.get("customerId"), filter.customerId()));
            }
            if (filter.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(builder.lessThan(root.get("createdAt"), filter.to()));
            }
            if (customerIds != null) {
                predicates.add(root.get("customerId").in(customerIds));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
