package com.tracker.dto;

import com.tracker.enums.BillingCycle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionRequest(
    @NotBlank String name,
    @NotNull BigDecimal amount,
    @NotNull BillingCycle billingCycle,
    @NotNull LocalDate nextRenewalDate
) {}
