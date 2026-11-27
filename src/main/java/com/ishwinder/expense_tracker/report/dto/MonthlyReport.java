package com.ishwinder.expense_tracker.report.dto;

import com.ishwinder.expense_tracker.budget.BudgetStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MonthlyReport(
        String month,
        BigDecimal totalSpent,
        BigDecimal overallBudget,        // null when no overall budget is set
        BigDecimal remaining,            // null when no overall budget; negative = overspent
        BigDecimal overallPercentUsed,   // null when no overall budget
        BudgetStatus overallStatus,
        List<CategoryLine> byCategory,
        List<DailyLine> dailyTotals
) {
    public record CategoryLine(Long categoryId, String name, String color,
                               BigDecimal spent, BigDecimal percentOfTotal,
                               BigDecimal budget,              // null when none set
                               BigDecimal budgetPercentUsed,   // null when none set
                               BudgetStatus budgetStatus) { }

    public record DailyLine(LocalDate date, BigDecimal total) { }
}