package com.tracker.dto;

import com.tracker.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResponse(
    Long id,
    BigDecimal amount,
    TransactionType type,
    LocalDate date,
    String description,
    String categoryName,
    LocalDateTime createdAt
) {}
