package com.ishwinder.expense_tracker.budget.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BudgetRequest(
        @NotBlank(message = "is required")
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "must look like 2026-10")
        String month,

        Long categoryId,       // omitted/null = overall budget for the month

        @NotNull(message = "is required")
        @Positive(message = "must be greater than 0")
        @Digits(integer = 10, fraction = 2,
                message = "must have at most 10 digits before and 2 after the decimal point")
        BigDecimal amount
) { }