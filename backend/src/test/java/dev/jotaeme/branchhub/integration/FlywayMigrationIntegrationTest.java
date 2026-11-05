package dev.jotaeme.branchhub.integration;

import dev.jotaeme.branchhub.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that Flyway, not Hibernate, built the schema the other context tests run on.
 */
class FlywayMigrationIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyBaselineMigrationExactlyOnce() {
        List<Map<String, Object>> versioned = jdbcTemplate.queryForList(
                "SELECT version, success FROM flyway_schema_history WHERE version IS NOT NULL");

        assertThat(versioned).hasSize(1);
        assertThat(versioned.getFirst())
                .containsEntry("version", "1")
                .containsEntry("success", true);
    }

    @Test
    void shouldCreateEveryApplicationTable() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = current_schema() AND table_type = 'BASE TABLE'",
                String.class);

        assertThat(tables).contains(
                "branch",
                "outbox_event",
                "processed_event",
                "iam_role",
                "iam_user",
                "iam_user_role",
                "iam_audit_log");
    }
}
