package dev.jotaeme.branchhub.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class AppMetrics {
    private final MeterRegistry meterRegistry;

    public AppMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public Timer.Sample startSample() {
        return Timer.start(meterRegistry);
    }

    public void recordUseCaseDuration(Timer.Sample sample, String useCase, String outcome) {
        sample.stop(Timer.builder("app_use_case_duration")
                .description("Use case execution duration")
                .tag("use_case", useCase)
                .tag("outcome", outcome)
                .publishPercentileHistogram()
                .register(meterRegistry));
    }

    public void incrementAuthLoginSuccess() {
        // Same tag keys as the failure counter: Prometheus rejects meters with one name but different tag keys.
        counter("app_auth_login_total", "success", "reason", "none").increment();
    }

    public void incrementAuthLoginFailure(String reason) {
        counter("app_auth_login_total", "failure", "reason", reason).increment();
    }

    public void incrementBranchCommand(String operation, String outcome) {
        counter("app_branch_command_total", outcome, "operation", operation).increment();
    }

    public void incrementOutboxPublish(String outcome) {
        counter("app_outbox_publish_total", outcome).increment();
    }

    public void incrementNotificationConsumer(String outcome) {
        counter("app_notification_consumer_total", outcome).increment();
    }

    public void incrementIamAdminAction(String action, String outcome) {
        counter("app_iam_admin_action_total", outcome, "action", action).increment();
    }

    private Counter counter(String metricName, String outcome) {
        return Counter.builder(metricName)
                .description("Business counter for application flows")
                .tag("outcome", outcome)
                .register(meterRegistry);
    }

    private Counter counter(String metricName, String outcome, String extraTag, String extraTagValue) {
        return Counter.builder(metricName)
                .description("Business counter for application flows")
                .tag("outcome", outcome)
                .tag(extraTag, extraTagValue)
                .register(meterRegistry);
    }
}
