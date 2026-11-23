package com.ishwinder.expense_tracker.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "is required")
        @Pattern(regexp = "^[A-Za-z0-9_.-]{3,30}$",
                message = "must be 3-30 characters: letters, digits, dot, dash or underscore")
        String username,

        @NotBlank(message = "is required")
        @Size(min = 8, max = 72, message = "must be 8 to 72 characters")
        String password
) { }