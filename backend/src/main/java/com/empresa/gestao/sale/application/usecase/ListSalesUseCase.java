package com.empresa.gestao.sale.application.usecase;

import com.empresa.gestao.sale.domain.repository.SaleFilter;
import com.empresa.gestao.sale.application.dto.SaleSummaryResult;
import com.empresa.gestao.sale.domain.repository.SaleRepository;
import com.empresa.gestao.shared.domain.query.PageQuery;
import com.empresa.gestao.shared.domain.query.PageResult;
import com.empresa.gestao.shared.domain.util.TextNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Service
public class ListSalesUseCase {

    private final SaleRepository saleRepository;

    public ListSalesUseCase(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<SaleSummaryResult> execute(UUID storeId, UUID customerId, LocalDate from,
                                                 LocalDate to, String search, Integer page,
                                                 Integer size, String sort) {
        Instant fromInstant = from == null ? null : from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to == null ? null : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        PageQuery query = PageQuery.of(page, size, sort, Map.of(
                "createdAt", "createdAt",
                "total", "total"
        ), "createdAt,desc");
        SaleFilter filter = new SaleFilter(storeId, customerId, fromInstant, toInstant,
                TextNormalizer.containsPattern(search));
        return saleRepository.search(filter, query).map(SaleResultMapper::toSummary);
    }
}
