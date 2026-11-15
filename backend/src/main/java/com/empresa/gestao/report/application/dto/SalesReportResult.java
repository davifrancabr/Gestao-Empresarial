package com.empresa.gestao.report.application.dto;

import com.empresa.gestao.sale.application.dto.SaleSummaryResult;
import com.empresa.gestao.shared.domain.query.PageResult;

import java.math.BigDecimal;

public record SalesReportResult(
        PageResult<SaleSummaryResult> sales,
        BigDecimal revenue,
        long salesCount
) {
}
