package com.ishwinder.expense_tracker.category;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CategoryApiIntegrationTest {

    private static final String URL = "/api/v1/categories";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    // Every test starts from a clean slate. User 1 (seeded by V2) is kept.
    @BeforeEach
    void cleanDatabase() {
        jdbc.update("DELETE FROM expenses");
        jdbc.update("DELETE FROM categories");
        jdbc.update("DELETE FROM users WHERE id <> 1");
    }

    @Test
    void create_returns201WithLocationAndBody() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Food", "color": "#FF8800" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Food"))
                .andExpect(jsonPath("$.color").value("#FF8800"));
    }

    @Test
    void list_returnsOnlyOwnCategories_sortedByName() throws Exception {
        insertCategory(1L, "Travel");
        insertCategory(1L, "Food");
        jdbc.update("INSERT INTO users (id, username, password_hash) VALUES (2, 'other', 'x')");
        insertCategory(2L, "NotMine");

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Food"))
                .andExpect(jsonPath("$[1].name").value("Travel"));
    }

    @Test
    void create_returns409_whenNameExistsWithDifferentCase() throws Exception {
        insertCategory(1L, "Food");

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "food" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void create_returns400WithFieldDetails_whenInputInvalid() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "   ", "color": "orange" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.length()").value(2));
    }

    @Test
    void update_renamesCategory() throws Exception {
        long id = insertCategory(1L, "Food");

        mockMvc.perform(put(URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Groceries", "color": "#00AA55" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Groceries"));

        String nameInDb = jdbc.queryForObject(
                "SELECT name FROM categories WHERE id = ?", String.class, id);
        assertThat(nameInDb).isEqualTo("Groceries");
    }

    @Test
    void update_returns404_whenCategoryDoesNotExist() throws Exception {
        mockMvc.perform(put(URL + "/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "X" }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void anotherUsersCategory_isInvisibleAndUntouchable() throws Exception {
        jdbc.update("INSERT INTO users (id, username, password_hash) VALUES (2, 'other', 'x')");
        long otherId = insertCategory(2L, "Secret");

        mockMvc.perform(put(URL + "/" + otherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Hacked" }
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete(URL + "/" + otherId))
                .andExpect(status().isNotFound());

        assertThat(countCategory(otherId)).isEqualTo(1);
    }

    @Test
    void delete_returns204_andRemovesCategory() throws Exception {
        long id = insertCategory(1L, "Food");

        mockMvc.perform(delete(URL + "/" + id))
                .andExpect(status().isNoContent());

        assertThat(countCategory(id)).isZero();
    }

    @Test
    void delete_returns409_whenCategoryHasExpenses() throws Exception {
        long id = insertCategory(1L, "Food");
        jdbc.update("""
                INSERT INTO expenses (user_id, category_id, amount, expense_date)
                VALUES (1, ?, 10.00, '2026-10-01')
                """, id);

        mockMvc.perform(delete(URL + "/" + id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Category is used by expenses and cannot be deleted"));

        assertThat(countCategory(id)).isEqualTo(1);   // still there
    }

    // ---- helpers: set up data directly in the database, bypassing the API ----

    private long insertCategory(long userId, String name) {
        jdbc.update("INSERT INTO categories (user_id, name) VALUES (?, ?)", userId, name);
        return jdbc.queryForObject(
                "SELECT id FROM categories WHERE user_id = ? AND name = ?",
                Long.class, userId, name);
    }

    private int countCategory(long id) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM categories WHERE id = ?", Integer.class, id);
    }
}