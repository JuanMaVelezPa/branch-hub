package com.jm.spring_web.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class LoginLockoutIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("companydb")
            .withUsername("company")
            .withPassword("company");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("security.jwt.secret", () -> "test-secret-test-secret-test-secret-123");
        registry.add("security.jwt.expiration-seconds", () -> "3600");
        registry.add("app.notifications.enabled", () -> "false");
        registry.add("app.security.bootstrap.enabled", () -> "false");
        registry.add("app.security.login.max-failed-attempts", () -> "3");
        registry.add("app.security.login.lockout-minutes", () -> "60");
    }

    @BeforeEach
    void seedAdminAndResetLockout() {
        // Same pattern as BranchFlowIntegrationTest: Testcontainers DB without Flyway in this harness.
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS iam_role (
                  id UUID PRIMARY KEY,
                  name VARCHAR(64) NOT NULL UNIQUE,
                  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS iam_user (
                  id UUID PRIMARY KEY,
                  email VARCHAR(320) UNIQUE,
                  email_verified_at TIMESTAMPTZ,
                  password_hash VARCHAR(255),
                  enabled BOOLEAN NOT NULL DEFAULT true,
                  failed_login_count INT NOT NULL DEFAULT 0,
                  locked_until TIMESTAMPTZ,
                  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                  mfa_enabled BOOLEAN NOT NULL DEFAULT false,
                  mfa_secret_encrypted VARCHAR(4000),
                  mfa_pending_secret_encrypted VARCHAR(4000)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS iam_user_role (
                  user_id UUID NOT NULL REFERENCES iam_user(id) ON DELETE CASCADE,
                  role_id UUID NOT NULL REFERENCES iam_role(id) ON DELETE CASCADE,
                  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                  PRIMARY KEY (user_id, role_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS iam_refresh_session (
                  id UUID PRIMARY KEY,
                  user_id UUID NOT NULL REFERENCES iam_user(id) ON DELETE CASCADE,
                  family_id UUID NOT NULL,
                  token_hash VARCHAR(64) NOT NULL,
                  expires_at TIMESTAMPTZ NOT NULL,
                  revoked_at TIMESTAMPTZ,
                  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
                )
                """);
        jdbcTemplate.execute(
                "CREATE UNIQUE INDEX IF NOT EXISTS iam_refresh_session_token_hash_uidx ON iam_refresh_session (token_hash)");

        jdbcTemplate.update("DELETE FROM iam_refresh_session");
        jdbcTemplate.update("DELETE FROM iam_user_role");
        jdbcTemplate.update("DELETE FROM iam_user");
        jdbcTemplate.update("DELETE FROM iam_role");

        Instant now = Instant.now();
        Timestamp ts = Timestamp.from(now);
        UUID superRoleId = UUID.randomUUID();
        UUID appRoleId = UUID.randomUUID();
        UUID userRoleId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO iam_role (id, name, created_at) VALUES (?, ?, ?)",
                superRoleId,
                "SUPER_ADMIN",
                ts);
        jdbcTemplate.update(
                "INSERT INTO iam_role (id, name, created_at) VALUES (?, ?, ?)",
                appRoleId,
                "APP_ADMIN",
                ts);
        jdbcTemplate.update(
                "INSERT INTO iam_role (id, name, created_at) VALUES (?, ?, ?)",
                userRoleId,
                "USER",
                ts);

        UUID userId = UUID.randomUUID();
        String hash = passwordEncoder.encode("admin123");
        jdbcTemplate.update(
                "INSERT INTO iam_user (id, email, email_verified_at, password_hash, enabled, failed_login_count, "
                        + "locked_until, created_at, updated_at) VALUES (?, ?, ?, ?, true, 0, NULL, ?, ?)",
                userId,
                "admin",
                ts,
                hash,
                ts,
                ts);
        jdbcTemplate.update(
                "INSERT INTO iam_user_role (user_id, role_id, created_at) VALUES (?, ?, ?)",
                userId,
                superRoleId,
                ts);
        jdbcTemplate.update(
                "INSERT INTO iam_user_role (user_id, role_id, created_at) VALUES (?, ?, ?)",
                userId,
                appRoleId,
                ts);
    }

    @Test
    void shouldLockAccountAfterRepeatedFailedPasswordsThenAllowLoginAfterClear() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"admin","password":"wrong"}
                                    """))
                    .andExpect(status().is(422));
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123"}
                                """))
                .andExpect(status().is(422));

        Integer failed = jdbcTemplate.queryForObject(
                "SELECT failed_login_count FROM iam_user WHERE LOWER(email) = LOWER(?)",
                Integer.class,
                "admin");
        assertThat(failed).isEqualTo(3);

        jdbcTemplate.update(
                "UPDATE iam_user SET failed_login_count = 0, locked_until = NULL WHERE LOWER(email) = LOWER(?)",
                "admin");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
