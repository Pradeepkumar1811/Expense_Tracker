package com.tracker.dto;

public record CategoryResponse(
    Long id,
    String name,
    boolean isDefault
) {}
