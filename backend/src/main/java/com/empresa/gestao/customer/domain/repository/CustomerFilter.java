package com.empresa.gestao.customer.domain.repository;

import java.util.UUID;

public record CustomerFilter(
        UUID storeId,
        boolean includeInactive,
        String searchTerm,
        String digitsTerm
) {
}
