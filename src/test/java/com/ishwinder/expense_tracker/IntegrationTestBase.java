package com.ishwinder.expense_tracker;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@WithUserDetails("dev")       // every request in these tests is made by the seeded user 1
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

    // A real browser must send a CSRF token on every changing request. These wrappers add
    // one, so existing tests keep reading naturally: post(URL), put(URL), delete(URL).
    // (Inherited methods win over the static imports that the test classes already have.)
    protected static MockHttpServletRequestBuilder get(String url, Object... vars) {
        return MockMvcRequestBuilders.get(url, vars);
    }

    protected static MockHttpServletRequestBuilder post(String url, Object... vars) {
        return MockMvcRequestBuilders.post(url, vars).with(csrf());
    }

    protected static MockHttpServletRequestBuilder put(String url, Object... vars) {
        return MockMvcRequestBuilders.put(url, vars).with(csrf());
    }

    protected static MockHttpServletRequestBuilder delete(String url, Object... vars) {
        return MockMvcRequestBuilders.delete(url, vars).with(csrf());
    }
}