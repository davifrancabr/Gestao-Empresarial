package com.empresa.gestao.sale.application.usecase;

import com.empresa.gestao.sale.domain.repository.SaleRepository;
import com.empresa.gestao.shared.domain.model.SaleStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class GetRevenueUseCase {

    private final SaleRepository saleRepository;

    public GetRevenueUseCase(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal execute(UUID storeId) {
        return saleRepository.sumRevenue(storeId, SaleStatus.CONCLUIDA);
    }

    @Transactional(readOnly = true)
    public BigDecimal execute(UUID storeId, LocalDate from, LocalDate to) {
        Instant fromInstant = from == null
                ? Instant.EPOCH
                : from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to == null
                ? Instant.parse("9999-12-31T00:00:00Z")
                : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return saleRepository.sumRevenue(storeId, SaleStatus.CONCLUIDA, fromInstant, toInstant);
    }
}
