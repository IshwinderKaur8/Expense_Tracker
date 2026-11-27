package com.ishwinder.expense_tracker.report;
import com.ishwinder.expense_tracker.budget.BudgetStatus;
import com.ishwinder.expense_tracker.budget.BudgetService;
import com.ishwinder.expense_tracker.budget.dto.BudgetResponse;
import com.ishwinder.expense_tracker.category.CategoryService;
import com.ishwinder.expense_tracker.category.dto.CategoryResponse;
import com.ishwinder.expense_tracker.common.BadRequestException;
import com.ishwinder.expense_tracker.expense.ExpenseStatsService;
import com.ishwinder.expense_tracker.expense.dto.CategoryTotal;
import com.ishwinder.expense_tracker.expense.dto.DailyTotal;
import com.ishwinder.expense_tracker.report.dto.MonthlyReport;

import com.ishwinder.expense_tracker.report.ReportService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate OCT_31 = LocalDate.of(2026, 10, 31);

    @Mock
    private ExpenseStatsService expenseStats;
    @Mock
    private BudgetService budgetService;
    @Mock
    private CategoryService categoryService;

    private ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(expenseStats, budgetService, categoryService);
    }

    @Test
    void monthly_computesTotalsPercentagesBudgetsAndSortsBySpendingDescending() {
        when(expenseStats.totalsByCategory(OCT_1, OCT_31)).thenReturn(List.of(
                new CategoryTotal(5L, new BigDecimal("1800.00")),
                new CategoryTotal(3L, new BigDecimal("7200.00"))));
        when(expenseStats.dailyTotals(OCT_1, OCT_31)).thenReturn(List.of(
                new DailyTotal(OCT_1, new BigDecimal("4000.00")),
                new DailyTotal(LocalDate.of(2026, 10, 15), new BigDecimal("5000.00"))));
        when(budgetService.list("2026-10")).thenReturn(List.of(
                new BudgetResponse(1L, "2026-10", null, null, new BigDecimal("25000.00")),
                new BudgetResponse(2L, "2026-10", 3L, "Food", new BigDecimal("8000.00")),
                new BudgetResponse(3L, "2026-10", 7L, "Health", new BigDecimal("500.00"))));
        when(categoryService.findByIds(any())).thenReturn(Map.of(
                3L, new CategoryResponse(3L, "Food", "#FF8800"),
                5L, new CategoryResponse(5L, "Travel", null),
                7L, new CategoryResponse(7L, "Health", null)));

        MonthlyReport report = service.monthly("2026-10");

        assertThat(report.totalSpent()).isEqualByComparingTo("9000.00");
        assertThat(report.overallBudget()).isEqualByComparingTo("25000.00");
        assertThat(report.remaining()).isEqualByComparingTo("16000.00");

        // sorted by spent (desc): Food, Travel, then the budget-only category Health
        assertThat(report.byCategory()).extracting(MonthlyReport.CategoryLine::name)
                .containsExactly("Food", "Travel", "Health");
        MonthlyReport.CategoryLine food = report.byCategory().get(0);
        assertThat(food.spent()).isEqualByComparingTo("7200.00");
        assertThat(food.percentOfTotal()).isEqualByComparingTo("80.00");
        assertThat(food.budget()).isEqualByComparingTo("8000.00");
        assertThat(food.color()).isEqualTo("#FF8800");
        assertThat(report.byCategory().get(1).budget()).isNull();       // Travel: no budget
        assertThat(report.byCategory().get(2).spent()).isEqualByComparingTo("0.00");
        assertThat(report.byCategory().get(2).percentOfTotal()).isEqualByComparingTo("0.00");

        // zero-filled for all 31 days of October
        assertThat(report.dailyTotals()).hasSize(31);
        assertThat(report.dailyTotals().get(0).total()).isEqualByComparingTo("4000.00");
        assertThat(report.dailyTotals().get(1).total()).isEqualByComparingTo("0.00");
        assertThat(report.dailyTotals().get(14).total()).isEqualByComparingTo("5000.00");
    }

    @Test
    void monthly_returnsZerosAndNoBudget_forAnEmptyMonth() {
        MonthlyReport report = service.monthly("2026-02");     // 2026 is not a leap year

        assertThat(report.totalSpent()).isEqualByComparingTo("0.00");
        assertThat(report.overallBudget()).isNull();
        assertThat(report.remaining()).isNull();
        assertThat(report.byCategory()).isEmpty();
        assertThat(report.dailyTotals()).hasSize(28);
        assertThat(report.dailyTotals()).allSatisfy(
                day -> assertThat(day.total()).isEqualByComparingTo("0.00"));
    }

    @Test
    void monthly_reportsNegativeRemaining_whenOverBudget() {
        when(expenseStats.totalsByCategory(OCT_1, OCT_31)).thenReturn(List.of(
                new CategoryTotal(3L, new BigDecimal("1200.00"))));
        when(budgetService.list("2026-10")).thenReturn(List.of(
                new BudgetResponse(1L, "2026-10", null, null, new BigDecimal("1000.00"))));
        when(categoryService.findByIds(any())).thenReturn(Map.of(
                3L, new CategoryResponse(3L, "Food", null)));

        MonthlyReport report = service.monthly("2026-10");

        assertThat(report.remaining()).isEqualByComparingTo("-200.00");
    }

    @Test
    void monthly_roundsPercentagesToTwoDecimals() {
        when(expenseStats.totalsByCategory(OCT_1, OCT_31)).thenReturn(List.of(
                new CategoryTotal(3L, new BigDecimal("100.00")),
                new CategoryTotal(5L, new BigDecimal("200.00"))));
        when(categoryService.findByIds(any())).thenReturn(Map.of(
                3L, new CategoryResponse(3L, "Food", null),
                5L, new CategoryResponse(5L, "Travel", null)));

        MonthlyReport report = service.monthly("2026-10");

        // 200/300 = 66.666...% and 100/300 = 33.333...%
        assertThat(report.byCategory().get(0).percentOfTotal()).isEqualByComparingTo("66.67");
        assertThat(report.byCategory().get(1).percentOfTotal()).isEqualByComparingTo("33.33");
    }

    @Test
    void monthly_throwsBadRequest_whenMonthIsInvalid() {
        assertThatThrownBy(() -> service.monthly("October"))
                .isInstanceOf(BadRequestException.class);
    }

        @Test
    void monthly_flagsWarningAndExceeded_againstBudgets() {
        when(expenseStats.totalsByCategory(OCT_1, OCT_31)).thenReturn(List.of(
                new CategoryTotal(3L, new BigDecimal("850.00")),
                new CategoryTotal(5L, new BigDecimal("300.00"))));
        when(budgetService.list("2026-10")).thenReturn(List.of(
                new BudgetResponse(1L, "2026-10", null, null, new BigDecimal("1000.00")),
                new BudgetResponse(2L, "2026-10", 3L, "Food", new BigDecimal("1000.00")),
                new BudgetResponse(3L, "2026-10", 5L, "Travel", new BigDecimal("250.00"))));
        when(categoryService.findByIds(any())).thenReturn(Map.of(
                3L, new CategoryResponse(3L, "Food", null),
                5L, new CategoryResponse(5L, "Travel", null)));

        MonthlyReport report = service.monthly("2026-10");

        // total 1150 of 1000
        assertThat(report.overallStatus()).isEqualTo(BudgetStatus.EXCEEDED);
        assertThat(report.overallPercentUsed()).isEqualByComparingTo("115.00");

        MonthlyReport.CategoryLine food = report.byCategory().stream()
                .filter(line -> line.name().equals("Food")).findFirst().orElseThrow();
        MonthlyReport.CategoryLine travel = report.byCategory().stream()
                .filter(line -> line.name().equals("Travel")).findFirst().orElseThrow();
        assertThat(food.budgetStatus()).isEqualTo(BudgetStatus.WARNING);     // 85% used
        assertThat(food.budgetPercentUsed()).isEqualByComparingTo("85.00");
        assertThat(travel.budgetStatus()).isEqualTo(BudgetStatus.EXCEEDED);  // 120% used
    }
}