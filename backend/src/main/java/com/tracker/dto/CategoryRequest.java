package com.tracker.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
    @NotBlank String name
) {}
