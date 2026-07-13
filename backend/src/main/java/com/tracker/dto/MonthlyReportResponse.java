package com.tracker.dto;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyReportResponse(
    Integer month,
    Integer year,
    BigDecimal totalIncome,
    BigDecimal totalExpenses,
    BigDecimal netSavings,
    List<CategoryBreakdown> categoryBreakdown
) {}
