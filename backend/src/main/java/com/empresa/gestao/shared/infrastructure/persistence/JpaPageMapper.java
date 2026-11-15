package com.empresa.gestao.shared.infrastructure.persistence;

import com.empresa.gestao.shared.domain.query.PageResult;
import org.springframework.data.domain.Page;

import java.util.function.Function;

public final class JpaPageMapper {

    private JpaPageMapper() {
    }

    public static <E, T> PageResult<T> toPageResult(Page<E> page, Function<E, T> mapper) {
        return new PageResult<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
