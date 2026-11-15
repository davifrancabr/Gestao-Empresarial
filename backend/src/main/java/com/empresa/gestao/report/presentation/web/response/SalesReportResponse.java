package com.empresa.gestao.report.presentation.web.response;

import com.empresa.gestao.report.application.dto.SalesReportResult;
import com.empresa.gestao.sale.application.dto.SaleSummaryResult;
import com.empresa.gestao.shared.presentation.web.PageResponse;

import java.math.BigDecimal;

public record SalesReportResponse(
        PageResponse<SaleSummaryResult> sales,
        BigDecimal revenue,
        long salesCount
) {

    public static SalesReportResponse from(SalesReportResult result) {
        return new SalesReportResponse(
                PageResponse.from(result.sales()),
                result.revenue(),
                result.salesCount()
        );
    }
}
