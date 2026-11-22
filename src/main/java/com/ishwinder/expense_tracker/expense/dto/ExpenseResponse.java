package com.ishwinder.expense_tracker.expense.dto;

import com.ishwinder.expense_tracker.expense.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseResponse(Long id, BigDecimal amount, Long categoryId, String categoryName,
                              LocalDate expenseDate, String description,
                              LocalDateTime createdAt) {

    public static ExpenseResponse from(Expense e, String categoryName) {
        return new ExpenseResponse(e.getId(), e.getAmount(), e.getCategoryId(), categoryName,
                e.getExpenseDate(), e.getDescription(), e.getCreatedAt());
    }
}