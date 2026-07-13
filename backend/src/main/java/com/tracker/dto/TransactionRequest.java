package com.tracker.dto;

import com.tracker.enums.TransactionType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
    @NotNull BigDecimal amount,
    @NotNull TransactionType type,
    @NotNull LocalDate date,
    String description,
    Long categoryId
) {}
