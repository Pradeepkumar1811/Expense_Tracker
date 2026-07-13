package com.tracker.dto;

import com.tracker.enums.BillingCycle;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionResponse(
    Long id,
    String name,
    BigDecimal amount,
    BillingCycle billingCycle,
    LocalDate nextRenewalDate
) {}
