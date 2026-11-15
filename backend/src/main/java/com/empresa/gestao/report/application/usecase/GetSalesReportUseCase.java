package com.empresa.gestao.report.application.usecase;

import com.empresa.gestao.report.application.dto.SalesReportResult;
import com.empresa.gestao.sale.application.dto.SaleSummaryResult;
import com.empresa.gestao.sale.application.usecase.GetRevenueUseCase;
import com.empresa.gestao.sale.application.usecase.ListSalesUseCase;
import com.empresa.gestao.shared.domain.query.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class GetSalesReportUseCase {

    private final ListSalesUseCase listSales;
    private final GetRevenueUseCase getRevenue;

    public GetSalesReportUseCase(ListSalesUseCase listSales, GetRevenueUseCase getRevenue) {
        this.listSales = listSales;
        this.getRevenue = getRevenue;
    }

    @Transactional(readOnly = true)
    public SalesReportResult execute(UUID storeId, UUID customerId, LocalDate from, LocalDate to,
                                     String search, Integer page, Integer size, String sort) {
        PageResult<SaleSummaryResult> sales = listSales.execute(storeId, customerId, from, to,
                search, page, size, sort);
        return new SalesReportResult(
                sales,
                getRevenue.execute(storeId, from, to),
                sales.totalElements()
        );
    }
}
