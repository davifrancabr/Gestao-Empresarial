package com.empresa.gestao.shared.infrastructure.persistence;

import com.empresa.gestao.shared.domain.query.PageQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableFactory {

    private PageableFactory() {
    }

    public static Pageable toPageable(PageQuery query) {
        Sort sort = Sort.by(query.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC,
                query.sortProperty());
        return PageRequest.of(query.page(), query.size(), sort);
    }
}
