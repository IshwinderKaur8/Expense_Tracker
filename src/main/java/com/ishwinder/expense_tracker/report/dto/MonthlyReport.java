package com.ishwinder.expense_tracker.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MonthlyReport(
        String month,
        BigDecimal totalSpent,
        BigDecimal overallBudget,      // null when no overall budget is set
        BigDecimal remaining,          // null when no overall budget; negative = overspent
        List<CategoryLine> byCategory,
        List<DailyLine> dailyTotals
) {
    public record CategoryLine(Long categoryId, String name, String color,
                               BigDecimal spent, BigDecimal percentOfTotal,
                               BigDecimal budget) { }       // budget null when none set

    public record DailyLine(LocalDate date, BigDecimal total) { }
}