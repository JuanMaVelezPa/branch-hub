package dev.jotaeme.branchhub.integration;

import dev.jotaeme.branchhub.infrastructure.security.DemoUserBootstrapRunner;
import dev.jotaeme.branchhub.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC 05 step 1, demo on: the demo user is seeded as APP_ADMIN only, and it is exempt from the
 * login lockout while every other account keeps it.
 */
@AutoConfigureMockMvc
class DemoUserIntegrationTest extends AbstractPostgresIntegrationTest {

    static final String DEMO_EMAIL = "demo@branchhub.test";
    static final String DEMO_PASSWORD = "Demo_Test_Pass_2026!";
    private static final String OTHER_EMAIL = "other-lockout@branchhub.test";
    private static final String OTHER_PASSWORD = "Other_Test_Pass_2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DemoUserBootstrapRunner demoUserBootstrapRunner;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.demo.enabled", () -> "true");
        registry.add("app.demo.user.email", () -> DEMO_EMAIL);
        registry.add("app.demo.user.password", () -> DEMO_PASSWORD);
        registry.add("app.security.login.max-failed-attempts", () -> "5");
        registry.add("app.security.login.lockout-minutes", () -> "15");
    }

    @BeforeEach
    void ensureDemoUser() {
        // Other classes share the database and may have wiped IAM rows; the runner is idempotent.
        demoUserBootstrapRunner.run(null);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM iam_user WHERE LOWER(email) IN (LOWER(?), LOWER(?))", DEMO_EMAIL, OTHER_EMAIL);
    }

    @Test
    void demoUserIsCreatedEnabledVerifiedWithAppAdminOnly() {
        jdbcTemplate.update("DELETE FROM iam_user WHERE LOWER(email) = LOWER(?)", DEMO_EMAIL);

        demoUserBootstrapRunner.run(null);

        var row = jdbcTemplate.queryForMap(
                "SELECT enabled, email_verified_at, failed_login_count, locked_until FROM iam_user "
                        + "WHERE LOWER(email) = LOWER(?)",
                DEMO_EMAIL);
        assertThat(row.get("enabled")).isEqualTo(true);
        assertThat(row.get("email_verified_at")).isNotNull();
        assertThat(row.get("failed_login_count")).isEqualTo(0);
        assertThat(row.get("locked_until")).isNull();
        assertThat(rolesOf(DEMO_EMAIL)).containsExactly("APP_ADMIN");
    }

    @Test
    void secondStartDoesNotDuplicateTheDemoUser() {
        demoUserBootstrapRunner.run(null);
        demoUserBootstrapRunner.run(null);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM iam_user WHERE LOWER(email) = LOWER(?)", Integer.class, DEMO_EMAIL);
        assertThat(count).isEqualTo(1);
        assertThat(rolesOf(DEMO_EMAIL)).containsExactly("APP_ADMIN");
    }

    @Test
    void demoUserIsNotLockedAfterSixWrongPasswords() throws Exception {
        for (int i = 0; i < 6; i++) {
            login(DEMO_EMAIL, "wrong-password").andExpect(status().isUnprocessableEntity());
        }

        var row = jdbcTemplate.queryForMap(
                "SELECT failed_login_count, locked_until FROM iam_user WHERE LOWER(email) = LOWER(?)", DEMO_EMAIL);
        assertThat(row.get("failed_login_count")).isEqualTo(0);
        assertThat(row.get("locked_until")).isNull();

        login(DEMO_EMAIL, DEMO_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void otherAccountsStillLockAfterFiveWrongPasswords() throws Exception {
        insertUser(OTHER_EMAIL, OTHER_PASSWORD);

        for (int i = 0; i < 5; i++) {
            login(OTHER_EMAIL, "wrong-password").andExpect(status().isUnprocessableEntity());
        }

        login(OTHER_EMAIL, OTHER_PASSWORD).andExpect(status().isUnprocessableEntity());
        Timestamp lockedUntil = jdbcTemplate.queryForObject(
                "SELECT locked_until FROM iam_user WHERE LOWER(email) = LOWER(?)", Timestamp.class, OTHER_EMAIL);
        assertThat(lockedUntil).isNotNull();
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s"}
                        """.formatted(email, password)));
    }

    private java.util.List<String> rolesOf(String email) {
        return jdbcTemplate.queryForList(
                "SELECT r.name FROM iam_user_role ur JOIN iam_role r ON r.id = ur.role_id "
                        + "JOIN iam_user u ON u.id = ur.user_id WHERE LOWER(u.email) = LOWER(?) ORDER BY r.name",
                String.class,
                email);
    }

    private void insertUser(String email, String rawPassword) {
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update(
                "INSERT INTO iam_user (id, email, email_verified_at, password_hash, enabled, failed_login_count, "
                        + "locked_until, created_at, updated_at) VALUES (?, ?, ?, ?, true, 0, NULL, ?, ?)",
                UUID.randomUUID(),
                email,
                now,
                passwordEncoder.encode(rawPassword),
                now,
                now);
    }
}
