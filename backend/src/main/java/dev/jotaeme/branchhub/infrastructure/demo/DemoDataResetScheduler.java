package dev.jotaeme.branchhub.infrastructure.demo;

import dev.jotaeme.branchhub.application.demo.usecase.ResetDemoDataUseCase;
import dev.jotaeme.branchhub.infrastructure.observability.AppMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.OptionalInt;
import java.util.function.Supplier;

/**
 * Resets the public demo data on a schedule ({@code app.demo.reset.cron} in {@code app.demo.reset.zone};
 * by default weekly, Saturdays 10:00 America/Bogota)
 * and seeds it once at startup when the branch table is empty.
 *
 * <p>{@code @EnableScheduling} is repeated here because the one on {@code KafkaOutboxConfig} is
 * conditional on notifications being on; the scheduled reset must not depend on that.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "app.demo.reset.enabled", havingValue = "true")
public class DemoDataResetScheduler {
    static final String OUTCOME_SUCCESS = "success";
    static final String OUTCOME_FAILURE = "failure";

    private static final Logger log = LoggerFactory.getLogger(DemoDataResetScheduler.class);

    private final ResetDemoDataUseCase resetDemoData;
    private final AppMetrics appMetrics;

    public DemoDataResetScheduler(ResetDemoDataUseCase resetDemoData, AppMetrics appMetrics) {
        this.resetDemoData = resetDemoData;
        this.appMetrics = appMetrics;
    }

    @Scheduled(cron = "${app.demo.reset.cron:0 0 10 * * SAT}", zone = "${app.demo.reset.zone:America/Bogota}")
    public void scheduledReset() {
        run("scheduled", () -> OptionalInt.of(resetDemoData.execute()));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedOnStartup() {
        run("startup", resetDemoData::seedIfEmpty);
    }

    /**
     * Never throws: a failed reset is logged and counted, and the app keeps serving.
     */
    private void run(String trigger, Supplier<OptionalInt> reset) {
        long start = System.nanoTime();
        try {
            OptionalInt branches = reset.get();
            long millis = (System.nanoTime() - start) / 1_000_000;
            if (branches.isEmpty()) {
                log.info("Demo data seed skipped: trigger={} reason=branch table not empty", trigger);
                return;
            }
            appMetrics.incrementDemoReset(OUTCOME_SUCCESS);
            log.info("Demo data reset completed: trigger={} branches={} durationMs={}",
                    trigger, branches.getAsInt(), millis);
        } catch (RuntimeException e) {
            appMetrics.incrementDemoReset(OUTCOME_FAILURE);
            log.error("Demo data reset failed: trigger={} cause={}", trigger, e.toString(), e);
        }
    }
}
