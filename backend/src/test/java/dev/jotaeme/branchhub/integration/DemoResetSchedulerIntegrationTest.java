package dev.jotaeme.branchhub.integration;

import dev.jotaeme.branchhub.infrastructure.demo.DemoDataResetScheduler;
import dev.jotaeme.branchhub.support.AbstractPostgresIntegrationTest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SPEC 05 step 4: startup seed, the scheduled reset and {@code app_demo_resets_total}. The cron is
 * not waited on: the tests call the scheduled method and publish {@link ApplicationReadyEvent}.
 */
class DemoResetSchedulerIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String DEMO_EMAIL = "demo-scheduler@branchhub.test";
    private static final String METRIC = "app_demo_resets_total";

    @Autowired
    private DemoDataResetScheduler scheduler;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private ConfigurableApplicationContext context;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.demo.enabled", () -> "true");
        registry.add("app.demo.user.email", () -> DEMO_EMAIL);
        registry.add("app.demo.user.password", () -> "Demo_Scheduler_Pass_2026!");
        registry.add("app.demo.reset.enabled", () -> "true");
        // Never fires while the tests run (midnight on 1 January).
        registry.add("app.demo.reset.cron", () -> "0 0 0 1 1 *");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("UPDATE iam_role SET name = 'APP_ADMIN' WHERE name = 'APP_ADMIN_RENAMED'");
        jdbc.update("DELETE FROM processed_event");
        jdbc.update("DELETE FROM outbox_event");
        jdbc.update("DELETE FROM branch");
        jdbc.update("DELETE FROM iam_user WHERE LOWER(email) = LOWER(?)", DEMO_EMAIL);
    }

    @Test
    void startupSeedsTheSampleBranchesWhenTheTableIsEmpty() {
        jdbc.update("DELETE FROM branch");

        publishApplicationReady();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM branch", Integer.class)).isEqualTo(12);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM branch WHERE is_active = false", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void startupLeavesANonEmptyTableAlone() {
        jdbc.update("DELETE FROM branch");
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbc.update(
                "INSERT INTO branch (id, code, name, city, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, true, ?, ?)",
                UUID.randomUUID(), "VISITOR-1", "Added today", "Bogotá", now, now);
        double successBefore = count("success");

        publishApplicationReady();

        assertThat(jdbc.queryForList("SELECT code FROM branch", String.class)).containsExactly("VISITOR-1");
        assertThat(count("success")).isEqualTo(successBefore);
    }

    @Test
    void scheduledResetIncrementsTheSuccessCounter() {
        double before = count("success");

        scheduler.scheduledReset();

        assertThat(count("success")).isEqualTo(before + 1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM branch", Integer.class)).isEqualTo(12);
    }

    @Test
    void failedResetIncrementsTheFailureCounterWithoutThrowing() {
        double before = count("failure");
        jdbc.update("UPDATE iam_role SET name = 'APP_ADMIN_RENAMED' WHERE name = 'APP_ADMIN'");

        scheduler.scheduledReset();

        assertThat(count("failure")).isEqualTo(before + 1);
    }

    private void publishApplicationReady() {
        context.publishEvent(new ApplicationReadyEvent(new SpringApplication(), new String[0], context, Duration.ZERO));
    }

    private double count(String outcome) {
        Counter counter = meterRegistry.find(METRIC).tag("outcome", outcome).counter();
        return counter == null ? 0 : counter.count();
    }
}
