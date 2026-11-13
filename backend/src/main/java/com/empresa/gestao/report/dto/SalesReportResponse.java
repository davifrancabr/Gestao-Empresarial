package com.empresa.gestao.report.dto;

import com.empresa.gestao.common.dto.PageResponse;
import com.empresa.gestao.sale.dto.SaleSummaryResponse;

import java.math.BigDecimal;

public record SalesReportResponse(
        PageResponse<SaleSummaryResponse> sales,
        BigDecimal revenue,
        long salesCount
) {
}
