package dev.jotaeme.branchhub.infrastructure.notification;

import dev.jotaeme.branchhub.infrastructure.persistence.OutboxEventJpaEntity;
import dev.jotaeme.branchhub.infrastructure.persistence.OutboxEventStatus;
import dev.jotaeme.branchhub.infrastructure.persistence.SpringDataOutboxEventRepository;
import dev.jotaeme.branchhub.infrastructure.observability.AppMetrics;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

@Service
@ConditionalOnProperty(name = "app.notifications.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayService {
    private static final Logger logger = LoggerFactory.getLogger(OutboxRelayService.class);

    private final SpringDataOutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AppMetrics appMetrics;
    private final String topicName;
    private final AtomicLong outboxLagSeconds = new AtomicLong(0L);

    public OutboxRelayService(
            SpringDataOutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            AppMetrics appMetrics,
            MeterRegistry meterRegistry,
            @Value("${app.kafka.topics.hr-notifications:hr-new-branch-notifications}") String topicName
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.appMetrics = appMetrics;
        this.topicName = topicName;
        Gauge.builder("outbox_lag", outboxLagSeconds, AtomicLong::get)
                .description("Age in seconds of the oldest pending outbox event")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay.fixed-delay-ms:5000}")
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEventJpaEntity> pendingEvents = outboxEventRepository
                .findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            outboxLagSeconds.set(0L);
            return;
        }

        updateLag(pendingEvents.getFirst().getCreatedAt());

        for (OutboxEventJpaEntity event : pendingEvents) {
            MDC.put("transactionId", event.getId().toString());
            MDC.put("eventType", event.getEventType());
            MDC.put("aggregateId", event.getAggregateId().toString());
            try {
                logger.info(
                        "Publishing outbox event eventId={} aggregateId={} eventType={} topic={}",
                        event.getId(),
                        event.getAggregateId(),
                        event.getEventType(),
                        topicName
                );
                // We only mark PROCESSED after broker ACK.
                Message<String> message = MessageBuilder.withPayload(event.getPayload())
                        .setHeader(KafkaHeaders.TOPIC, topicName)
                        .setHeader(KafkaHeaders.KEY, event.getAggregateId().toString())
                        .setHeader("x-transaction-id", event.getId().toString())
                        .build();
                kafkaTemplate.send(message)
                        .get(10, TimeUnit.SECONDS);
                event.setStatus(OutboxEventStatus.PROCESSED);
                event.setProcessedAt(LocalDateTime.now());
                outboxEventRepository.save(event);
                appMetrics.incrementOutboxPublish("success");
                logger.info(
                        "Outbox event marked as PROCESSED eventId={} topic={}",
                        event.getId(),
                        topicName
                );
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                event.setRetryCount(event.getRetryCount() + 1);
                outboxEventRepository.save(event);
                appMetrics.incrementOutboxPublish("failure");
                logger.error("Outbox relay interrupted while publishing event {}", event.getId(), ex);
            } catch (ExecutionException | TimeoutException ex) {
                event.setRetryCount(event.getRetryCount() + 1);
                outboxEventRepository.save(event);
                appMetrics.incrementOutboxPublish("failure");
                logger.error("Failed to publish outbox event {} to topic {}", event.getId(), topicName, ex);
            } finally {
                MDC.remove("transactionId");
                MDC.remove("eventType");
                MDC.remove("aggregateId");
            }
        }
    }

    private void updateLag(LocalDateTime oldestCreatedAt) {
        long lag = Math.max(0L, Duration.between(oldestCreatedAt, LocalDateTime.now()).getSeconds());
        outboxLagSeconds.set(lag);
    }
}
