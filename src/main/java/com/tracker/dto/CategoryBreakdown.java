package com.tracker.dto;

import java.math.BigDecimal;

public record CategoryBreakdown(
    String categoryName,
    BigDecimal total
) {}
