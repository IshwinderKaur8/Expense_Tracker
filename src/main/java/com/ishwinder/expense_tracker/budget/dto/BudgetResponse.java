package com.ishwinder.expense_tracker.budget.dto;

import com.ishwinder.expense_tracker.budget.Budget;

import java.math.BigDecimal;
import java.time.YearMonth;

public record BudgetResponse(Long id, String month, Long categoryId, String categoryName,
                             BigDecimal amount) {

    public static BudgetResponse from(Budget b, String categoryName) {
        return new BudgetResponse(b.getId(), YearMonth.from(b.getBudgetMonth()).toString(),
                b.getCategoryId(), categoryName, b.getAmount());
    }
}