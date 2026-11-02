package com.ishwinder.expense_tracker.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 50, message = "must be at most 50 characters")
        String name,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "must look like #RRGGBB")
        String color
) { }