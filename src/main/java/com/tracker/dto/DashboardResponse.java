package com.tracker.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
    BigDecimal currentMonthIncome,
    BigDecimal currentMonthExpenses,
    BigDecimal netBalance,
    int activeSubscriptionCount,
    BigDecimal totalSubscriptionCost,
    BigDecimal budgetUtilizationPercent,
    List<SubscriptionResponse> upcomingRenewals
) {}
