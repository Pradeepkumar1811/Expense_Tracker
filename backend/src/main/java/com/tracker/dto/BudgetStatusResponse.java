package com.tracker.dto;

import java.math.BigDecimal;

public record BudgetStatusResponse(
    Long id,
    Integer month,
    Integer year,
    BigDecimal limitAmount,
    BigDecimal totalSpending,
    BigDecimal remainingAmount,
    boolean overspent,
    BigDecimal overspentAmount,
    String categoryName
) {}
