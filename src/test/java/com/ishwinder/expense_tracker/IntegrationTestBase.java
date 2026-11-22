package com.ishwinder.expense_tracker;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    // Every test starts from a clean slate. User 1 (seeded by V2) is kept.
    @BeforeEach
    protected void cleanDatabase() {
        jdbc.update("DELETE FROM budgets");
        jdbc.update("DELETE FROM expenses");
        jdbc.update("DELETE FROM categories");
        jdbc.update("DELETE FROM users WHERE id <> 1");
    }

    protected long insertCategory(long userId, String name) {
        jdbc.update("INSERT INTO categories (user_id, name) VALUES (?, ?)", userId, name);
        return jdbc.queryForObject(
                "SELECT id FROM categories WHERE user_id = ? AND name = ?",
                Long.class, userId, name);
    }
}