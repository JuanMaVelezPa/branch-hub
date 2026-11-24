package dev.jotaeme.branchhub.infrastructure.persistence.demo;

import dev.jotaeme.branchhub.application.demo.model.DemoBranch;
import dev.jotaeme.branchhub.application.demo.port.DemoDataPort;
import dev.jotaeme.branchhub.infrastructure.demo.DemoProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Plain JDBC on purpose: the reset is maintenance, not a domain write, so it skips the use cases,
 * JPA and the outbox. Runs inside the caller's transaction.
 */
@Component
public class JdbcDemoDataAdapter implements DemoDataPort {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public JdbcDemoDataAdapter(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void resetBranches(List<DemoBranch> branches) {
        jdbc.update("DELETE FROM processed_event");
        jdbc.update("DELETE FROM outbox_event");
        jdbc.update("DELETE FROM branch");

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbc.batchUpdate(
                "INSERT INTO branch (id, code, name, city, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                branches,
                branches.size(),
                (ps, branch) -> {
                    ps.setObject(1, UUID.randomUUID());
                    ps.setString(2, branch.code());
                    ps.setString(3, branch.name());
                    ps.setString(4, branch.city());
                    ps.setBoolean(5, branch.active());
                    ps.setTimestamp(6, now);
                    ps.setTimestamp(7, now);
                });
    }

    @Override
    public void restoreDemoUser(String email, String rawPassword) {
        UUID roleId = jdbc.query(
                        "SELECT id FROM iam_role WHERE name = ?",
                        (rs, n) -> rs.getObject("id", UUID.class),
                        DemoProperties.DEMO_ROLE)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Role " + DemoProperties.DEMO_ROLE + " not found"));

        String passwordHash = passwordEncoder.encode(rawPassword);
        Timestamp now = Timestamp.from(Instant.now());

        UUID userId = jdbc.query(
                        "SELECT id FROM iam_user WHERE LOWER(email) = LOWER(?)",
                        (rs, n) -> rs.getObject("id", UUID.class),
                        email)
                .stream()
                .findFirst()
                .orElse(null);

        if (userId == null) {
            userId = UUID.randomUUID();
            jdbc.update(
                    "INSERT INTO iam_user (id, email, email_verified_at, password_hash, enabled, failed_login_count, "
                            + "locked_until, created_at, updated_at) VALUES (?, ?, ?, ?, true, 0, NULL, ?, ?)",
                    userId, email, now, passwordHash, now, now);
        } else {
            jdbc.update(
                    "UPDATE iam_user SET enabled = true, password_hash = ?, failed_login_count = 0, locked_until = NULL, "
                            + "email_verified_at = COALESCE(email_verified_at, ?), updated_at = ? WHERE id = ?",
                    passwordHash, now, now, userId);
        }

        jdbc.update("DELETE FROM iam_user_role WHERE user_id = ?", userId);
        jdbc.update("INSERT INTO iam_user_role (user_id, role_id, created_at) VALUES (?, ?, ?)", userId, roleId, now);
    }

    @Override
    public boolean isBranchTableEmpty() {
        Boolean exists = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM branch)", Boolean.class);
        return !Boolean.TRUE.equals(exists);
    }
}
