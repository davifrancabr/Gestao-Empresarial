package com.empresa.gestao.shared.domain.query;

import java.util.Map;

/**
 * Parâmetros de paginação independentes de framework. O campo de ordenação já
 * chega traduzido para a propriedade persistida, respeitando a whitelist.
 */
public record PageQuery(
        int page,
        int size,
        String sortProperty,
        boolean ascending
) {

    public static PageQuery of(Integer page, Integer size, String sort,
                               Map<String, String> allowedSorts, String defaultSort) {
        int safePage = page == null ? 0 : Math.max(page, 0);
        int safeSize = size == null ? 10 : Math.min(Math.max(size, 1), 100);

        String requested = sort == null || sort.isBlank() ? defaultSort : sort;
        String[] parts = requested.split(",");
        String field = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim() : "asc";
        if (!allowedSorts.containsKey(field)) {
            field = defaultSort.split(",")[0];
            direction = defaultSort.split(",").length > 1 ? defaultSort.split(",")[1] : "asc";
        }
        boolean ascending = !"desc".equalsIgnoreCase(direction);
        return new PageQuery(safePage, safeSize, allowedSorts.get(field), ascending);
    }
}
