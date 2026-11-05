package dev.jotaeme.branchhub.infrastructure.notification;

import dev.jotaeme.branchhub.infrastructure.observability.AppMetrics;
import dev.jotaeme.branchhub.infrastructure.persistence.OutboxEventJpaEntity;
import dev.jotaeme.branchhub.infrastructure.persistence.OutboxEventStatus;
import dev.jotaeme.branchhub.infrastructure.persistence.SpringDataOutboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboxRelayServiceTest {

    @Test
    void shouldPublishAndMarkOutboxEventAsProcessedAfterAck() {
        SpringDataOutboxEventRepository repository = Mockito.mock(SpringDataOutboxEventRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        AppMetrics appMetrics = Mockito.mock(AppMetrics.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        OutboxRelayService service = new OutboxRelayService(
                repository,
                kafkaTemplate,
                appMetrics,
                meterRegistry,
                "hr-new-branch-notifications"
        );

        OutboxEventJpaEntity event = pendingEvent(LocalDateTime.now().minusSeconds(60));
        Mockito.when(repository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING))
                .thenReturn(List.of(event));
        CompletableFuture<SendResult<String, String>> ack = CompletableFuture.completedFuture(null);
        Mockito.when(kafkaTemplate.send(Mockito.any(Message.class)))
                .thenReturn(ack);

        service.relayPendingEvents();

        ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        Mockito.verify(repository, Mockito.atLeastOnce()).save(captor.capture());
        OutboxEventJpaEntity saved = captor.getValue();
        assertEquals(OutboxEventStatus.PROCESSED, saved.getStatus());
        assertNotNull(saved.getProcessedAt());
        assertEquals(0, saved.getRetryCount());
        assertTrue(meterRegistry.get("outbox_lag").gauge().value() >= 1d);
    }

    @Test
    void shouldIncreaseRetryCountWhenKafkaPublishFails() {
        SpringDataOutboxEventRepository repository = Mockito.mock(SpringDataOutboxEventRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        AppMetrics appMetrics = Mockito.mock(AppMetrics.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        OutboxRelayService service = new OutboxRelayService(
                repository,
                kafkaTemplate,
                appMetrics,
                meterRegistry,
                "hr-new-branch-notifications"
        );

        OutboxEventJpaEntity event = pendingEvent(LocalDateTime.now().minusSeconds(30));
        Mockito.when(repository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING))
                .thenReturn(List.of(event));
        CompletableFuture<SendResult<String, String>> failedAck = new CompletableFuture<>();
        failedAck.completeExceptionally(new RuntimeException("kafka unavailable"));
        Mockito.when(kafkaTemplate.send(Mockito.any(Message.class)))
                .thenReturn(failedAck);

        service.relayPendingEvents();

        ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        Mockito.verify(repository, Mockito.atLeastOnce()).save(captor.capture());
        OutboxEventJpaEntity saved = captor.getValue();
        assertEquals(OutboxEventStatus.PENDING, saved.getStatus());
        assertEquals(1, saved.getRetryCount());
    }

    private OutboxEventJpaEntity pendingEvent(LocalDateTime createdAt) {
        return OutboxEventJpaEntity.builder()
                .id(UUID.randomUUID())
                .aggregateType("BRANCH")
                .aggregateId(UUID.randomUUID())
                .eventType("BRANCH_CREATED")
                .payload("{\"eventId\":\"" + UUID.randomUUID() + "\"}")
                .status(OutboxEventStatus.PENDING)
                .retryCount(0)
                .createdAt(createdAt)
                .build();
    }
}
