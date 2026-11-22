package com.ishwinder.expense_tracker.expense.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotNull(message = "is required")
        @Positive(message = "must be greater than 0")
        @Digits(integer = 10, fraction = 2,
                message = "must have at most 10 digits before and 2 after the decimal point")
        BigDecimal amount,

        @NotNull(message = "is required")
        Long categoryId,

        @NotNull(message = "is required")
        @PastOrPresent(message = "must not be in the future")
        LocalDate expenseDate,

        @Size(max = 255, message = "must be at most 255 characters")
        String description
) { }