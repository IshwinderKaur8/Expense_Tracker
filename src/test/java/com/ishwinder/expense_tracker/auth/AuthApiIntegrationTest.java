package com.ishwinder.expense_tracker.auth;

import com.ishwinder.expense_tracker.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Deliberately NOT logged in by default: these tests exercise the real login flow.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthApiIntegrationTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private LoginAttemptService loginAttempts;                      // NEW (1 of 3)

    @BeforeEach
    void cleanDatabase() {
        loginAttempts.clear();                                      // NEW (2 of 3)
        jdbc.update("DELETE FROM budgets");
        jdbc.update("DELETE FROM expenses");
        jdbc.update("DELETE FROM categories");
        jdbc.update("DELETE FROM users");
    }

    @Test
    void register_returns201_andStoresOnlyAHash() throws Exception {
        register("alice", PASSWORD);

        String stored = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE username = 'alice'", String.class);
        assertThat(stored).startsWith("$2").isNotEqualTo(PASSWORD);   // BCrypt hashes start with $2
    }

    @Test
    void register_returns409_forDuplicateUsername_ignoringCase() throws Exception {
        register("alice", PASSWORD);

        mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("ALICE", PASSWORD)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_returns400_withDetails_whenInputInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("a b", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.length()").value(2));
    }

    @Test
    void login_thenMe_returnsTheLoggedInUsername() throws Exception {
        register("alice", PASSWORD);
        MockHttpSession session = login("alice", PASSWORD);

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void login_returns401_forWrongPassword_andForUnknownUser() throws Exception {
        register("alice", PASSWORD);

        mockMvc.perform(formLogin("/api/v1/auth/login").user("alice").password("wrong-password"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(formLogin("/api/v1/auth/login").user("nobody").password(PASSWORD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returns429_afterFiveFailures_evenWithTheCorrectPassword() throws Exception {   // NEW (3 of 3)
        register("alice", PASSWORD);
        register("bob", PASSWORD);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(formLogin("/api/v1/auth/login").user("alice").password("wrong-" + i))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(formLogin("/api/v1/auth/login").user("alice").password(PASSWORD))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));

        // someone else is unaffected
        mockMvc.perform(formLogin("/api/v1/auth/login").user("bob").password(PASSWORD))
                .andExpect(status().isOk());
    }

    @Test
    void api_returns401_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/v1/categories")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/reports/monthly").param("month", "2026-10"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void changingRequests_return403_withoutCsrfToken_evenWhenLoggedIn() throws Exception {
        register("alice", PASSWORD);
        MockHttpSession session = login("alice", PASSWORD);

        mockMvc.perform(post("/api/v1/categories").session(session)             // no csrf()
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Food" }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/categories").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Food" }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void usersCannotSeeOrTouchEachOthersData() throws Exception {
        register("alice", PASSWORD);
        register("bob", PASSWORD);
        MockHttpSession alice = login("alice", PASSWORD);
        MockHttpSession bob = login("bob", PASSWORD);

        mockMvc.perform(post("/api/v1/categories").session(alice).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Secret" }
                                """))
                .andExpect(status().isCreated());
        long secretId = jdbc.queryForObject(
                "SELECT id FROM categories WHERE name = 'Secret'", Long.class);

        mockMvc.perform(get("/api/v1/categories").session(alice))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/v1/categories").session(bob))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(put("/api/v1/categories/" + secretId).session(bob).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Hacked" }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void logout_returns204_andInvalidatesTheSession() throws Exception {
        register("alice", PASSWORD);
        MockHttpSession session = login("alice", PASSWORD);

        mockMvc.perform(post("/api/v1/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(session.isInvalid()).isTrue();
    }

    // ---- helpers ----

    private String registerJson(String username, String password) {
        return """
                { "username": "%s", "password": "%s" }
                """.formatted(username, password);
    }

    private void register(String username, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(username, password)))
                .andExpect(status().isCreated());
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc
                .perform(formLogin("/api/v1/auth/login").user(username).password(password))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession();
    }
}