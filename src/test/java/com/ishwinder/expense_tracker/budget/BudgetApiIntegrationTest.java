package com.ishwinder.expense_tracker.budget;

import com.ishwinder.expense_tracker.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BudgetApiIntegrationTest extends IntegrationTestBase {

    private static final String URL = "/api/v1/budgets";

    @Test
    void set_replacesOverallBudget_insteadOfCreatingADuplicate() throws Exception {
        putBudget("""
                { "month": "2026-10", "amount": 25000 }
                """).andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-10"))
                .andExpect(jsonPath("$.categoryId").doesNotExist());

        putBudget("""
                { "month": "2026-10", "amount": 30000.50 }
                """).andExpect(status().isOk());

        // MySQL's unique key can't catch this (NULLs are distinct), so the service must
        Integer rows = jdbc.queryForObject("""
                SELECT COUNT(*) FROM budgets
                WHERE user_id = 1 AND budget_month = '2026-10-01' AND category_id IS NULL
                """, Integer.class);
        BigDecimal amount = jdbc.queryForObject(
                "SELECT amount FROM budgets WHERE user_id = 1 AND category_id IS NULL",
                BigDecimal.class);
        assertThat(rows).isEqualTo(1);
        assertThat(amount).isEqualByComparingTo("30000.50");
    }

    @Test
    void set_keepsOverallAndCategoryBudgetsSeparate_andListShowsOverallFirst() throws Exception {
        long foodId = insertCategory(1L, "Food");
        long travelId = insertCategory(1L, "Travel");

        putBudget("""
                { "month": "2026-10", "categoryId": %d, "amount": 3000 }
                """.formatted(travelId)).andExpect(status().isOk());
        putBudget("""
                { "month": "2026-10", "categoryId": %d, "amount": 8000 }
                """.formatted(foodId)).andExpect(status().isOk());
        putBudget("""
                { "month": "2026-10", "amount": 25000 }
                """).andExpect(status().isOk());

        mockMvc.perform(get(URL).param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].categoryId").doesNotExist())
                .andExpect(jsonPath("$[1].categoryName").value("Food"))
                .andExpect(jsonPath("$[2].categoryName").value("Travel"));
    }

    @Test
    void list_onlyReturnsTheRequestedMonth_forTheCurrentUser() throws Exception {
        jdbc.update("INSERT INTO users (id, username, password_hash) VALUES (2, 'other', 'x')");
        insertBudget(1L, null, "2026-10-01", "100.00");
        insertBudget(1L, null, "2026-11-01", "200.00");
        insertBudget(2L, null, "2026-10-01", "999.00");

        mockMvc.perform(get(URL).param("month", "2026-10"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].amount").value(100.00));
    }

    @Test
    void set_returns404_whenCategoryDoesNotExist() throws Exception {
        putBudget("""
                { "month": "2026-10", "categoryId": 999999, "amount": 500 }
                """).andExpect(status().isNotFound());
    }

    @Test
    void set_returns400WithFieldDetails_whenInputInvalid() throws Exception {
        putBudget("""
                { "month": "2026-13", "amount": -5 }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.length()").value(2));
    }

    @Test
    void list_returns400_whenMonthMissingOrInvalid() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MISSING_PARAMETER"));

        mockMvc.perform(get(URL).param("month", "October"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void delete_returns204_andRemovesBudget() throws Exception {
        long id = insertBudget(1L, null, "2026-10-01", "100.00");

        mockMvc.perform(delete(URL + "/" + id)).andExpect(status().isNoContent());

        assertThat(countBudget(id)).isZero();
    }

    @Test
    void anotherUsersBudget_cannotBeDeleted() throws Exception {
        jdbc.update("INSERT INTO users (id, username, password_hash) VALUES (2, 'other', 'x')");
        long id = insertBudget(2L, null, "2026-10-01", "100.00");

        mockMvc.perform(delete(URL + "/" + id)).andExpect(status().isNotFound());

        assertThat(countBudget(id)).isEqualTo(1);
    }

    @Test
    void deletingACategory_alsoRemovesItsBudgets() throws Exception {
        long foodId = insertCategory(1L, "Food");
        long budgetId = insertBudget(1L, foodId, "2026-10-01", "8000.00");

        mockMvc.perform(delete("/api/v1/categories/" + foodId))
                .andExpect(status().isNoContent());

        assertThat(countBudget(budgetId)).isZero();     // ON DELETE CASCADE did this
    }

    // ---- helpers ----

    private org.springframework.test.web.servlet.ResultActions putBudget(String json)
            throws Exception {
        return mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long insertBudget(long userId, Long categoryId, String month, String amount) {
        jdbc.update("""
                INSERT INTO budgets (user_id, category_id, budget_month, amount)
                VALUES (?, ?, ?, ?)
                """, userId, categoryId, month, new BigDecimal(amount));
        return jdbc.queryForObject("SELECT MAX(id) FROM budgets", Long.class);
    }

    private int countBudget(long id) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM budgets WHERE id = ?", Integer.class, id);
    }
}