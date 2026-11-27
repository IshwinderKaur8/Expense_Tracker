package com.ishwinder.expense_tracker.report;

import com.ishwinder.expense_tracker.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportApiIntegrationTest extends IntegrationTestBase {

    private static final String URL = "/api/v1/reports/monthly";

    @Test
    void monthly_aggregatesOnlyTheRequestedMonth_forTheCurrentUser() throws Exception {
        long food = insertCategory(1L, "Food");
        long travel = insertCategory(1L, "Travel");
        long health = insertCategory(1L, "Health");          // budget only, no spending

        insertExpense(1L, food, "999.00", "2026-09-30");     // day before: excluded
        insertExpense(1L, food, "100.00", "2026-10-01");     // first day: included
        insertExpense(1L, food, "50.50", "2026-10-01");
        insertExpense(1L, travel, "349.50", "2026-10-15");
        insertExpense(1L, food, "100.00", "2026-10-31");     // last day: included
        insertExpense(1L, food, "999.00", "2026-11-01");     // day after: excluded

        jdbc.update("INSERT INTO users (id, username, password_hash) VALUES (2, 'other', 'x')");
        long secret = insertCategory(2L, "Secret");
        insertExpense(2L, secret, "5000.00", "2026-10-10");  // another user: excluded

        insertBudget(null, "1000.00");
        insertBudget(food, "400.00");
        insertBudget(health, "500.00");

        mockMvc.perform(get(URL).param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-10"))
                .andExpect(jsonPath("$.totalSpent").value(600.00))
                .andExpect(jsonPath("$.overallBudget").value(1000.00))
                .andExpect(jsonPath("$.remaining").value(400.00))
                // Travel 349.50, Food 250.50, Health 0.00 (sorted by spending)
                .andExpect(jsonPath("$.byCategory.length()").value(3))
                .andExpect(jsonPath("$.byCategory[0].name").value("Travel"))
                .andExpect(jsonPath("$.byCategory[0].percentOfTotal").value(58.25))
                .andExpect(jsonPath("$.byCategory[1].name").value("Food"))
                .andExpect(jsonPath("$.byCategory[1].spent").value(250.50))
                .andExpect(jsonPath("$.byCategory[1].percentOfTotal").value(41.75))
                .andExpect(jsonPath("$.byCategory[1].budget").value(400.00))
                .andExpect(jsonPath("$.byCategory[2].name").value("Health"))
                .andExpect(jsonPath("$.byCategory[2].spent").value(0.0))
                // daily series: all 31 days, with the right values on the right days
                .andExpect(jsonPath("$.dailyTotals.length()").value(31))
                .andExpect(jsonPath("$.dailyTotals[0].date").value("2026-10-01"))
                .andExpect(jsonPath("$.dailyTotals[0].total").value(150.50))
                .andExpect(jsonPath("$.dailyTotals[1].total").value(0.0))
                .andExpect(jsonPath("$.dailyTotals[14].total").value(349.50))
                .andExpect(jsonPath("$.dailyTotals[30].date").value("2026-10-31"))
                .andExpect(jsonPath("$.dailyTotals[30].total").value(100.00));
    }

    @Test
    void monthly_returnsZerosAndNullBudget_whenMonthHasNoData() throws Exception {
        mockMvc.perform(get(URL).param("month", "2026-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpent").value(0.0))
                .andExpect(jsonPath("$.overallBudget").doesNotExist())
                .andExpect(jsonPath("$.remaining").doesNotExist())
                .andExpect(jsonPath("$.byCategory.length()").value(0))
                .andExpect(jsonPath("$.dailyTotals.length()").value(31));
    }

    @Test
    void monthly_returns400_whenMonthIsMissingOrInvalid() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MISSING_PARAMETER"));

        mockMvc.perform(get(URL).param("month", "2026-13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

        @Test
    void monthly_marksWarningAndExceeded_forBudgets() throws Exception {
        long food = insertCategory(1L, "Food");
        long travel = insertCategory(1L, "Travel");
        insertExpense(1L, food, "85.00", "2026-10-05");      // 85% of 100  -> WARNING
        insertExpense(1L, travel, "165.00", "2026-10-06");   // 110% of 150 -> EXCEEDED
        insertBudget(null, "200.00");                        // 250 of 200  -> EXCEEDED
        insertBudget(food, "100.00");
        insertBudget(travel, "150.00");

        mockMvc.perform(get(URL).param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallStatus").value("EXCEEDED"))
                .andExpect(jsonPath("$.overallPercentUsed").value(125.0))
                .andExpect(jsonPath("$.byCategory[?(@.name=='Food')].budgetStatus").value("WARNING"))
                .andExpect(jsonPath("$.byCategory[?(@.name=='Travel')].budgetStatus").value("EXCEEDED"));
    }

    // ---- helpers ----

    private void insertExpense(long userId, long categoryId, String amount, String date) {
        jdbc.update("""
                INSERT INTO expenses (user_id, category_id, amount, expense_date)
                VALUES (?, ?, ?, ?)
                """, userId, categoryId, new BigDecimal(amount), date);
    }

    private void insertBudget(Long categoryId, String amount) {
        jdbc.update("""
                INSERT INTO budgets (user_id, category_id, budget_month, amount)
                VALUES (1, ?, '2026-10-01', ?)
                """, categoryId, new BigDecimal(amount));
    }
}