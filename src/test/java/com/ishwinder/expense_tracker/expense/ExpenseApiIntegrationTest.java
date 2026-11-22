package com.ishwinder.expense_tracker.expense;

import com.ishwinder.expense_tracker.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ExpenseApiIntegrationTest {

    private static final String URL = "/api/v1/expenses";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;

    private long foodId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM expenses");
        jdbc.update("DELETE FROM categories");
        jdbc.update("DELETE FROM users WHERE id <> 1");
        foodId = insertCategory(1L, "Food");
    }

    @Test
    void create_returns201_withCategoryNameAndTrimmedDescription() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 249.50, "categoryId": %d,
                                  "expenseDate": "2026-10-31", "description": "  Lunch  " }
                                """.formatted(foodId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(249.50))
                .andExpect(jsonPath("$.categoryName").value("Food"))
                .andExpect(jsonPath("$.description").value("Lunch"));
    }

    @Test
    void create_returns400_withAllFieldProblems() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 0, "expenseDate": "2999-01-01" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.length()").value(3));   // amount, categoryId, date
    }

    @Test
    void create_returns400_whenMoreThanTwoDecimalPlaces() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 1.234, "categoryId": %d, "expenseDate": "2026-10-31" }
                                """.formatted(foodId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns400_withOurErrorShape_whenDateIsMalformed() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 5, "categoryId": %d, "expenseDate": "yesterday" }
                                """.formatted(foodId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    void create_returns404_whenCategoryDoesNotExistOrBelongsToSomeoneElse() throws Exception {
        jdbc.update("INSERT INTO users (id, username, password_hash) VALUES (2, 'other', 'x')");
        long othersCategory = insertCategory(2L, "Secret");

        for (long categoryId : new long[]{999999L, othersCategory}) {
            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "amount": 5, "categoryId": %d, "expenseDate": "2026-10-31" }
                                    """.formatted(categoryId)))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void list_returnsNewestFirst_andAppliesDateAndCategoryFilters() throws Exception {
        long travelId = insertCategory(1L, "Travel");
        insertExpense(foodId, "10.00", "2026-10-01");
        insertExpense(foodId, "20.00", "2026-10-15");
        insertExpense(travelId, "30.00", "2026-10-10");

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].expenseDate").value("2026-10-15"))
                .andExpect(jsonPath("$.items[2].expenseDate").value("2026-10-01"));

        mockMvc.perform(get(URL).param("from", "2026-10-05").param("to", "2026-10-20"))
                .andExpect(jsonPath("$.totalItems").value(2));

        mockMvc.perform(get(URL).param("categoryId", String.valueOf(travelId)))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].categoryName").value("Travel"));
    }

    @Test
    void list_paginates() throws Exception {
        for (int day = 1; day <= 25; day++) {
            insertExpense(foodId, "1.00", "2026-10-%02d".formatted(day));
        }

        mockMvc.perform(get(URL).param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(10))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalItems").value(25))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void list_returns400_whenFromIsAfterTo_orDateIsInvalid() throws Exception {
        mockMvc.perform(get(URL).param("from", "2026-10-20").param("to", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));

        mockMvc.perform(get(URL).param("from", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
    }

    @Test
    void update_changesFieldsInDatabase() throws Exception {
        long travelId = insertCategory(1L, "Travel");
        long id = insertExpense(foodId, "10.00", "2026-10-01");

        mockMvc.perform(put(URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "amount": 75, "categoryId": %d, "expenseDate": "2026-10-02",
                                  "description": "Train" }
                                """.formatted(travelId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("Travel"));

        BigDecimal amountInDb = jdbc.queryForObject(
                "SELECT amount FROM expenses WHERE id = ?", BigDecimal.class, id);
        assertThat(amountInDb).isEqualByComparingTo("75.00");
    }

    @Test
    void delete_returns204_andRemovesExpense() throws Exception {
        long id = insertExpense(foodId, "10.00", "2026-10-01");

        mockMvc.perform(delete(URL + "/" + id)).andExpect(status().isNoContent());

        assertThat(countExpense(id)).isZero();
    }

    @Test
    void anotherUsersExpense_isInvisibleAndUntouchable() throws Exception {
        jdbc.update("INSERT INTO users (id, username, password_hash) VALUES (2, 'other', 'x')");
        long othersCategory = insertCategory(2L, "Secret");
        jdbc.update("""
                INSERT INTO expenses (user_id, category_id, amount, expense_date)
                VALUES (2, ?, 5.00, '2026-10-01')
                """, othersCategory);
        long id = jdbc.queryForObject(
                "SELECT id FROM expenses WHERE user_id = 2", Long.class);

        mockMvc.perform(get(URL + "/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(delete(URL + "/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(get(URL)).andExpect(jsonPath("$.totalItems").value(0));
        assertThat(countExpense(id)).isEqualTo(1);
    }

    // ---- helpers ----

    private long insertCategory(long userId, String name) {
        jdbc.update("INSERT INTO categories (user_id, name) VALUES (?, ?)", userId, name);
        return jdbc.queryForObject(
                "SELECT id FROM categories WHERE user_id = ? AND name = ?",
                Long.class, userId, name);
    }

    private long insertExpense(long categoryId, String amount, String date) {
        jdbc.update("""
                INSERT INTO expenses (user_id, category_id, amount, expense_date)
                VALUES (1, ?, ?, ?)
                """, categoryId, new BigDecimal(amount), date);
        return jdbc.queryForObject("SELECT MAX(id) FROM expenses", Long.class);
    }

    private int countExpense(long id) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM expenses WHERE id = ?", Integer.class, id);
    }
}