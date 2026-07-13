package com.tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReminderResponse(
    Long id,
    String subscriptionName,
    LocalDate renewalDate,
    BigDecimal amount,
    boolean read,
    LocalDateTime createdAt
) {}
