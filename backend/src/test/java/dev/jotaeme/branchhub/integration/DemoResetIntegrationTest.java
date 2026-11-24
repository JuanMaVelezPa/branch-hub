package dev.jotaeme.branchhub.integration;

import dev.jotaeme.branchhub.application.demo.usecase.ResetDemoDataUseCase;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC 05 step 3: the reset brings branches, the outbox and the demo user back to the known state,
 * in one transaction, without touching other IAM data.
 */
@AutoConfigureMockMvc
class DemoResetIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String DEMO_EMAIL = "demo-reset@branchhub.test";
    private static final String DEMO_PASSWORD = "Demo_Reset_Pass_2026!";
    private static final String OTHER_EMAIL = "other-reset@branchhub.test";
    private static final List<String> SAMPLE_CODES =
            IntStream.rangeClosed(1, 12).mapToObj(i -> "BR-%03d".formatted(i)).toList();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ResetDemoDataUseCase resetDemoDataUseCase;

    @Autowired
    private DemoUserBootstrapRunner demoUserBootstrapRunner;

    private UUID auditRowId;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.demo.enabled", () -> "true");
        registry.add("app.demo.user.email", () -> DEMO_EMAIL);
        registry.add("app.demo.user.password", () -> DEMO_PASSWORD);
    }

    @BeforeEach
    void startFromSampleData() {
        demoUserBootstrapRunner.run(null);
        resetDemoDataUseCase.execute();
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("UPDATE iam_role SET name = 'APP_ADMIN' WHERE name = 'APP_ADMIN_RENAMED'");
        jdbc.update("DELETE FROM processed_event");
        jdbc.update("DELETE FROM outbox_event");
        jdbc.update("DELETE FROM branch");
        jdbc.update("DELETE FROM iam_user WHERE LOWER(email) IN (LOWER(?), LOWER(?))", DEMO_EMAIL, OTHER_EMAIL);
        if (auditRowId != null) {
            jdbc.update("DELETE FROM iam_audit_log WHERE id = ?", auditRowId);
        }
    }

    @Test
    void resetRestoresSampleBranchesOutboxAndDemoUserAndLeavesOtherIamDataAlone() throws Exception {
        messUpEverything();
        var otherIamBefore = otherIamSnapshot();

        int inserted = resetDemoDataUseCase.execute();

        assertThat(inserted).isEqualTo(12);
        assertSampleBranches();
        assertThat(count("outbox_event")).isZero();
        assertThat(count("processed_event")).isZero();

        var demo = jdbc.queryForMap(
                "SELECT enabled, failed_login_count, locked_until FROM iam_user WHERE LOWER(email) = LOWER(?)",
                DEMO_EMAIL);
        assertThat(demo.get("enabled")).isEqualTo(true);
        assertThat(demo.get("failed_login_count")).isEqualTo(0);
        assertThat(demo.get("locked_until")).isNull();
        assertThat(rolesOf(DEMO_EMAIL)).containsExactly("APP_ADMIN");

        assertThat(otherIamSnapshot()).isEqualTo(otherIamBefore);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(DEMO_EMAIL, DEMO_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void resetRecreatesAMissingDemoUser() {
        jdbc.update("DELETE FROM iam_user WHERE LOWER(email) = LOWER(?)", DEMO_EMAIL);

        resetDemoDataUseCase.execute();

        assertThat(rolesOf(DEMO_EMAIL)).containsExactly("APP_ADMIN");
        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM iam_user WHERE LOWER(email) = LOWER(?)", String.class, DEMO_EMAIL);
        assertThat(passwordEncoder.matches(DEMO_PASSWORD, hash)).isTrue();
    }

    @Test
    void failedResetRollsBackTheBranchChanges() {
        insertBranch("EXTRA-001", "Visitor Branch", "Nowhere", true);
        // The demo user step fails when the APP_ADMIN role cannot be found.
        jdbc.update("UPDATE iam_role SET name = 'APP_ADMIN_RENAMED' WHERE name = 'APP_ADMIN'");

        assertThatThrownBy(() -> resetDemoDataUseCase.execute()).isInstanceOf(IllegalStateException.class);

        jdbc.update("UPDATE iam_role SET name = 'APP_ADMIN' WHERE name = 'APP_ADMIN_RENAMED'");
        assertThat(count("branch")).isEqualTo(13);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM branch WHERE code = 'EXTRA-001'", Integer.class))
                .isEqualTo(1);
    }

    private void messUpEverything() {
        insertBranch("EXTRA-001", "Visitor Branch", "Nowhere", true);
        jdbc.update("UPDATE branch SET name = 'Renamed by a visitor' WHERE code = 'BR-001'");
        jdbc.update("UPDATE branch SET is_active = false WHERE code = 'BR-002'");

        UUID branchId = jdbc.queryForObject("SELECT id FROM branch WHERE code = 'EXTRA-001'", UUID.class);
        jdbc.update(
                "INSERT INTO outbox_event (id, aggregate_type, aggregate_id, event_type, payload, status, retry_count, "
                        + "created_at) VALUES (?, 'BRANCH', ?, 'BRANCH_CREATED', '{}', 'PENDING', 0, ?)",
                UUID.randomUUID(), branchId, Timestamp.valueOf(LocalDateTime.now()));
        jdbc.update("INSERT INTO processed_event (event_id, processed_at) VALUES (?, ?)",
                UUID.randomUUID(), Timestamp.valueOf(LocalDateTime.now()));

        jdbc.update(
                "UPDATE iam_user SET enabled = false, failed_login_count = 4, locked_until = ? "
                        + "WHERE LOWER(email) = LOWER(?)",
                Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)), DEMO_EMAIL);
        jdbc.update(
                "INSERT INTO iam_user_role (user_id, role_id) SELECT u.id, r.id FROM iam_user u, iam_role r "
                        + "WHERE LOWER(u.email) = LOWER(?) AND r.name = 'SUPER_ADMIN'",
                DEMO_EMAIL);
        assertThat(rolesOf(DEMO_EMAIL)).containsExactly("APP_ADMIN", "SUPER_ADMIN");

        Timestamp now = Timestamp.from(Instant.now());
        UUID otherId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO iam_user (id, email, email_verified_at, password_hash, enabled, failed_login_count, "
                        + "locked_until, created_at, updated_at) VALUES (?, ?, ?, ?, false, 2, NULL, ?, ?)",
                otherId, OTHER_EMAIL, now, passwordEncoder.encode("Other_Pass_2026!"), now, now);
        jdbc.update(
                "INSERT INTO iam_user_role (user_id, role_id) SELECT ?, id FROM iam_role WHERE name = 'USER'",
                otherId);

        auditRowId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO iam_audit_log (id, actor_user_id, action, target_user_id, metadata) VALUES (?, ?, ?, ?, ?)",
                auditRowId, otherId, "TEST_ACTION", otherId, "{}");
    }

    /** Every IAM row that is not the demo user's, so a change to the SUPER_ADMIN shows up too. */
    private Map<String, List<Map<String, Object>>> otherIamSnapshot() {
        return Map.of(
                "users", jdbc.queryForList(
                        "SELECT id, email, email_verified_at, password_hash, enabled, failed_login_count, locked_until, "
                                + "created_at, updated_at FROM iam_user WHERE LOWER(email) <> LOWER(?) ORDER BY id",
                        DEMO_EMAIL),
                "userRoles", jdbc.queryForList(
                        "SELECT ur.user_id, ur.role_id, ur.created_at FROM iam_user_role ur JOIN iam_user u "
                                + "ON u.id = ur.user_id WHERE LOWER(u.email) <> LOWER(?) ORDER BY ur.user_id, ur.role_id",
                        DEMO_EMAIL),
                "roles", jdbc.queryForList("SELECT id, name, created_at FROM iam_role ORDER BY id"),
                "audit", jdbc.queryForList("SELECT * FROM iam_audit_log ORDER BY id"));
    }

    private void assertSampleBranches() {
        var rows = jdbc.queryForList("SELECT code, name, is_active FROM branch ORDER BY code");
        assertThat(rows).extracting(r -> r.get("code")).containsExactlyElementsOf(SAMPLE_CODES);
        assertThat(rows).filteredOn(r -> Boolean.FALSE.equals(r.get("is_active"))).hasSize(2);
        assertThat(rows).extracting(r -> r.get("name")).doesNotContain("Renamed by a visitor");
        assertThat(rows.get(1).get("is_active")).isEqualTo(true);
    }

    private void insertBranch(String code, String name, String city, boolean active) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbc.update(
                "INSERT INTO branch (id, code, name, city, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), code, name, city, active, now, now);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private List<String> rolesOf(String email) {
        return jdbc.queryForList(
                "SELECT r.name FROM iam_user_role ur JOIN iam_role r ON r.id = ur.role_id "
                        + "JOIN iam_user u ON u.id = ur.user_id WHERE LOWER(u.email) = LOWER(?) ORDER BY r.name",
                String.class,
                email);
    }
}
