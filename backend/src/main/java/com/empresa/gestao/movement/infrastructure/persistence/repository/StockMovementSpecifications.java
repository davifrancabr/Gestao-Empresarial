package com.empresa.gestao.movement.infrastructure.persistence.repository;

import com.empresa.gestao.movement.domain.repository.MovementFilter;
import com.empresa.gestao.movement.infrastructure.persistence.entity.StockMovementJpaEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Filtros dinâmicos do histórico de movimentações. Os filtros opcionais são
 * adicionados somente quando informados, evitando parâmetros nulos no SQL.
 */
public final class StockMovementSpecifications {

    private StockMovementSpecifications() {
    }

    public static Specification<StockMovementJpaEntity> filtered(MovementFilter filter,
                                                                 Collection<java.util.UUID> productIds) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("storeId"), filter.storeId()));

            if (filter.productId() != null) {
                predicates.add(builder.equal(root.get("productId"), filter.productId()));
            }
            if (filter.type() != null) {
                predicates.add(builder.equal(root.get("type"), filter.type()));
            }
            if (filter.from() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(builder.lessThan(root.get("createdAt"), filter.to()));
            }
            if (productIds != null) {
                predicates.add(root.get("productId").in(productIds));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
