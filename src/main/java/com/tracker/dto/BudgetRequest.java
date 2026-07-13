package com.tracker.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BudgetRequest(
    @NotNull Integer month,
    @NotNull Integer year,
    @NotNull BigDecimal limitAmount,
    Long categoryId
) {}
