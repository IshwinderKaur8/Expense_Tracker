package com.ishwinder.expense_tracker.report;

import com.ishwinder.expense_tracker.budget.BudgetService;
import com.ishwinder.expense_tracker.budget.dto.BudgetResponse;
import com.ishwinder.expense_tracker.category.CategoryService;
import com.ishwinder.expense_tracker.category.dto.CategoryResponse;
import com.ishwinder.expense_tracker.common.Months;
import com.ishwinder.expense_tracker.expense.ExpenseStatsService;
import com.ishwinder.expense_tracker.expense.dto.CategoryTotal;
import com.ishwinder.expense_tracker.expense.dto.DailyTotal;
import com.ishwinder.expense_tracker.report.dto.MonthlyReport;
import com.ishwinder.expense_tracker.report.dto.MonthlyReport.CategoryLine;
import com.ishwinder.expense_tracker.report.dto.MonthlyReport.DailyLine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ExpenseStatsService expenseStats;
    private final BudgetService budgetService;
    private final CategoryService categoryService;

    public ReportService(ExpenseStatsService expenseStats, BudgetService budgetService,
                         CategoryService categoryService) {
        this.expenseStats = expenseStats;
        this.budgetService = budgetService;
        this.categoryService = categoryService;
    }

    // One transaction = one consistent snapshot for every query below
    @Transactional(readOnly = true)
    public MonthlyReport monthly(String month) {
        YearMonth yearMonth = Months.parse(month);
        LocalDate from = yearMonth.atDay(1);
        LocalDate to = yearMonth.atEndOfMonth();

        List<CategoryTotal> totals = expenseStats.totalsByCategory(from, to);
        List<BudgetResponse> budgets = budgetService.list(yearMonth.toString());

        BigDecimal totalSpent = totals.stream()
                .map(CategoryTotal::total)
                .reduce(ZERO, BigDecimal::add);

        BigDecimal overallBudget = budgets.stream()
                .filter(b -> b.categoryId() == null)
                .map(BudgetResponse::amount)
                .findFirst()
                .orElse(null);
        BigDecimal remaining = overallBudget == null ? null : overallBudget.subtract(totalSpent);

        Map<Long, BigDecimal> spentByCategory = totals.stream()
                .collect(Collectors.toMap(CategoryTotal::categoryId, CategoryTotal::total));
        Map<Long, BigDecimal> budgetByCategory = budgets.stream()
                .filter(b -> b.categoryId() != null)
                .collect(Collectors.toMap(BudgetResponse::categoryId, BudgetResponse::amount));

        // categories with spending OR a budget this month
        Set<Long> categoryIds = new HashSet<>(spentByCategory.keySet());
        categoryIds.addAll(budgetByCategory.keySet());
        Map<Long, CategoryResponse> categories = categoryService.findByIds(categoryIds);

        List<CategoryLine> byCategory = categoryIds.stream()
                .map(id -> {
                    BigDecimal spent = spentByCategory.getOrDefault(id, ZERO);
                    CategoryResponse category = categories.get(id);
                    return new CategoryLine(
                            id,
                            category != null ? category.name() : "Unknown",
                            category != null ? category.color() : null,
                            spent,
                            percentOf(spent, totalSpent),
                            budgetByCategory.get(id));
                })
                .sorted(Comparator.comparing(CategoryLine::spent).reversed()
                        .thenComparing(CategoryLine::name))
                .toList();

        // every day of the month, zero-filled, so charts get a continuous series
        Map<LocalDate, BigDecimal> spentByDay = expenseStats.dailyTotals(from, to).stream()
                .collect(Collectors.toMap(DailyTotal::date, DailyTotal::total));
        List<DailyLine> dailyTotals = from.datesUntil(to.plusDays(1))
                .map(day -> new DailyLine(day, spentByDay.getOrDefault(day, ZERO)))
                .toList();

        return new MonthlyReport(yearMonth.toString(), totalSpent, overallBudget, remaining,
                byCategory, dailyTotals);
    }

    private BigDecimal percentOf(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) {
            return ZERO;                        // avoid dividing by zero in an empty month
        }
        return part.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP);
    }
}