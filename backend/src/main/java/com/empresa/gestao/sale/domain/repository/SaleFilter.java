package com.empresa.gestao.sale.domain.repository;

import java.time.Instant;
import java.util.UUID;

public record SaleFilter(
        UUID storeId,
        UUID customerId,
        Instant from,
        Instant to,
        String searchTerm
) {
}
