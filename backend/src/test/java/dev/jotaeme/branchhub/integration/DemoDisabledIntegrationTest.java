package dev.jotaeme.branchhub.integration;

import dev.jotaeme.branchhub.application.demo.usecase.ResetDemoDataUseCase;
import dev.jotaeme.branchhub.infrastructure.demo.DemoDataResetScheduler;
import dev.jotaeme.branchhub.infrastructure.security.DemoUserBootstrapRunner;
import dev.jotaeme.branchhub.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC 05 step 1, demo off. Runs on the default test configuration ({@code app.demo.*} unset, so
 * the demo is off and the lockout is the default 5 attempts): no demo user is created and an email
 * that would be the demo one locks like any other account.
 */
@AutoConfigureMockMvc
class DemoDisabledIntegrationTest extends AbstractPostgresIntegrationTest {

    // Distinct from DemoUserIntegrationTest's email: the database is shared across test classes.
    private static final String DEMO_EMAIL = "demo-off@branchhub.test";
    private static final String DEMO_PASSWORD = "Demo_Off_Pass_2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DemoUserBootstrapRunner demoUserBootstrapRunner;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    @AfterEach
    void removeDemoEmail() {
        jdbcTemplate.update("DELETE FROM iam_user WHERE LOWER(email) = LOWER(?)", DEMO_EMAIL);
    }

    @Test
    void noDemoUserIsCreatedWhenDisabled() {
        demoUserBootstrapRunner.run(null);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM iam_user WHERE LOWER(email) = LOWER(?)", Integer.class, DEMO_EMAIL);
        assertThat(count).isZero();
    }

    @Test
    void noResetUseCaseOrSchedulerWhenDisabled() {
        assertThat(applicationContext.getBeanNamesForType(ResetDemoDataUseCase.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(DemoDataResetScheduler.class)).isEmpty();
    }

    @Test
    void demoEmailLocksLikeAnyOtherAccountWhenDisabled() throws Exception {
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update(
                "INSERT INTO iam_user (id, email, email_verified_at, password_hash, enabled, failed_login_count, "
                        + "locked_until, created_at, updated_at) VALUES (?, ?, ?, ?, true, 0, NULL, ?, ?)",
                UUID.randomUUID(),
                DEMO_EMAIL,
                now,
                passwordEncoder.encode(DEMO_PASSWORD),
                now,
                now);

        for (int i = 0; i < 5; i++) {
            login(DEMO_EMAIL, "wrong-password").andExpect(status().isUnprocessableEntity());
        }

        login(DEMO_EMAIL, DEMO_PASSWORD).andExpect(status().isUnprocessableEntity());
        Timestamp lockedUntil = jdbcTemplate.queryForObject(
                "SELECT locked_until FROM iam_user WHERE LOWER(email) = LOWER(?)", Timestamp.class, DEMO_EMAIL);
        assertThat(lockedUntil).isNotNull();
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s"}
                        """.formatted(email, password)));
    }
}
