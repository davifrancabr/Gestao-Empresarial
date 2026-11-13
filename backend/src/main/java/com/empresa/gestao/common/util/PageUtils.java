package com.empresa.gestao.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

public final class PageUtils {

    private PageUtils() {
    }

    public static Pageable create(Integer page, Integer size, String sort,
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
        Sort.Direction dir = "desc".equalsIgnoreCase(direction)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return PageRequest.of(safePage, safeSize, Sort.by(dir, allowedSorts.get(field)));
    }
}
