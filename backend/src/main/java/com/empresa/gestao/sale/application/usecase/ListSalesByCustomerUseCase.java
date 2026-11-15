package com.empresa.gestao.sale.application.usecase;

import com.empresa.gestao.sale.application.dto.SaleSummaryResult;
import com.empresa.gestao.sale.domain.repository.SaleRepository;
import com.empresa.gestao.shared.domain.query.PageQuery;
import com.empresa.gestao.shared.domain.query.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class ListSalesByCustomerUseCase {

    private final SaleRepository saleRepository;

    public ListSalesByCustomerUseCase(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<SaleSummaryResult> execute(UUID storeId, UUID customerId,
                                                 Integer page, Integer size) {
        PageQuery query = PageQuery.of(page, size, "createdAt,desc", Map.of(
                "createdAt", "createdAt",
                "total", "total"
        ), "createdAt,desc");
        return saleRepository.findByCustomer(storeId, customerId, query)
                .map(SaleResultMapper::toSummary);
    }
}
