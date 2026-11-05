package dev.jotaeme.branchhub.infrastructure.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jotaeme.branchhub.application.notification.model.BranchCreatedNotificationEvent;
import dev.jotaeme.branchhub.infrastructure.observability.AppMetrics;
import dev.jotaeme.branchhub.infrastructure.persistence.ProcessedEventJpaEntity;
import dev.jotaeme.branchhub.infrastructure.persistence.SpringDataProcessedEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.notifications.enabled", havingValue = "true", matchIfMissing = true)
public class HrBranchNotificationConsumer {
    private static final Logger logger = LoggerFactory.getLogger(HrBranchNotificationConsumer.class);

    private final ObjectMapper objectMapper;
    private final HrEmailService hrEmailService;
    private final SpringDataProcessedEventRepository processedEventRepository;
    private final AppMetrics appMetrics;
    private final Counter failedNotificationsCounter;

    public HrBranchNotificationConsumer(
            ObjectMapper objectMapper,
            HrEmailService hrEmailService,
            SpringDataProcessedEventRepository processedEventRepository,
            AppMetrics appMetrics,
            MeterRegistry meterRegistry
    ) {
        this.objectMapper = objectMapper;
        this.hrEmailService = hrEmailService;
        this.processedEventRepository = processedEventRepository;
        this.appMetrics = appMetrics;
        this.failedNotificationsCounter = Counter.builder("failed_notifications")
                .description("Number of notifications that reached DLT")
                .register(meterRegistry);
    }

    @RetryableTopic(
            attempts = "${app.kafka.hr-consumer.retry-attempts:4}",
            backOff = @BackOff(
                    delayString = "${app.kafka.hr-consumer.retry-delay-ms:1000}",
                    multiplierString = "${app.kafka.hr-consumer.retry-multiplier:2.0}",
                    maxDelayString = "${app.kafka.hr-consumer.retry-max-delay-ms:30000}"
            ),
            kafkaTemplate = "kafkaTemplate"
    )
    @KafkaListener(
            topics = "${app.kafka.topics.hr-notifications:hr-new-branch-notifications}",
            groupId = "${spring.kafka.consumer.group-id:rrhh-notifier}"
    )
    @Transactional
    public void consume(
            String payload,
            @Header(value = "x-transaction-id", required = false) String transactionId,
            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic
    ) throws Exception {
        BranchCreatedNotificationEvent event = objectMapper.readValue(payload, BranchCreatedNotificationEvent.class);
        UUID eventId = event.eventId();
        MDC.put("transactionId", transactionId != null ? transactionId : eventId.toString());
        MDC.put("eventType", "BRANCH_CREATED");
        if (topic != null) {
            MDC.put("topic", topic);
        }

        try {
            logger.info(
                    "Received branch notification eventId={} branchId={} code={}",
                    event.eventId(),
                    event.branchId(),
                    event.code()
            );
            appMetrics.incrementNotificationConsumer("received");
            if (processedEventRepository.existsById(eventId)) {
                logger.info("Skipping duplicate notification event {}", eventId);
                appMetrics.incrementNotificationConsumer("duplicate");
                return;
            }

            hrEmailService.sendNewBranchEmail(event);
            processedEventRepository.save(ProcessedEventJpaEntity.builder()
                    .eventId(eventId)
                    .processedAt(LocalDateTime.now())
                    .build());
            appMetrics.incrementNotificationConsumer("processed");
            logger.info("Notification processed successfully eventId={}", eventId);
        } finally {
            MDC.remove("transactionId");
            MDC.remove("eventType");
            MDC.remove("topic");
        }
    }

    @DltHandler
    public void onDlt(String payload) {
        failedNotificationsCounter.increment();
        appMetrics.incrementNotificationConsumer("dlt");
        logger.error("Notification moved to DLT payload={}", payload);
    }
}
