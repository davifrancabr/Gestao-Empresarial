package com.empresa.gestao.shared.domain.query;

import com.empresa.gestao.shared.domain.query.PageQuery;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PageResult<T> empty(PageQuery query) {
        return new PageResult<>(List.of(), query.page(), query.size(), 0, 0,
                query.page() == 0, true);
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(), page, size,
                totalElements, totalPages, first, last);
    }
}
