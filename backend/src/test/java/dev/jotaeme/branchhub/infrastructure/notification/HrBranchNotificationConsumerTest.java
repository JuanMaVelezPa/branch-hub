package dev.jotaeme.branchhub.infrastructure.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jotaeme.branchhub.application.notification.model.BranchCreatedNotificationEvent;
import dev.jotaeme.branchhub.infrastructure.observability.AppMetrics;
import dev.jotaeme.branchhub.infrastructure.persistence.ProcessedEventJpaEntity;
import dev.jotaeme.branchhub.infrastructure.persistence.SpringDataProcessedEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HrBranchNotificationConsumerTest {

    @Test
    void shouldSendEmailAndStoreProcessedEventWhenMessageIsNew() throws Exception {
        ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
        HrEmailService hrEmailService = Mockito.mock(HrEmailService.class);
        SpringDataProcessedEventRepository processedEventRepository = Mockito.mock(SpringDataProcessedEventRepository.class);
        AppMetrics appMetrics = Mockito.mock(AppMetrics.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        HrBranchNotificationConsumer consumer = new HrBranchNotificationConsumer(
                objectMapper,
                hrEmailService,
                processedEventRepository,
                appMetrics,
                meterRegistry
        );

        BranchCreatedNotificationEvent event = event(UUID.randomUUID());
        Mockito.when(objectMapper.readValue(Mockito.anyString(), Mockito.eq(BranchCreatedNotificationEvent.class)))
                .thenReturn(event);
        Mockito.when(processedEventRepository.existsById(event.eventId())).thenReturn(false);

        consumer.consume("{\"eventId\":\"" + event.eventId() + "\"}", event.eventId().toString(), "hr-new-branch-notifications");

        Mockito.verify(hrEmailService).sendNewBranchEmail(event);
        ArgumentCaptor<ProcessedEventJpaEntity> captor = ArgumentCaptor.forClass(ProcessedEventJpaEntity.class);
        Mockito.verify(processedEventRepository).save(captor.capture());
        assertEquals(event.eventId(), captor.getValue().getEventId());
        assertNotNull(captor.getValue().getProcessedAt());
    }

    @Test
    void shouldSkipDuplicateEventWithoutSendingEmail() throws Exception {
        ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
        HrEmailService hrEmailService = Mockito.mock(HrEmailService.class);
        SpringDataProcessedEventRepository processedEventRepository = Mockito.mock(SpringDataProcessedEventRepository.class);
        AppMetrics appMetrics = Mockito.mock(AppMetrics.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        HrBranchNotificationConsumer consumer = new HrBranchNotificationConsumer(
                objectMapper,
                hrEmailService,
                processedEventRepository,
                appMetrics,
                meterRegistry
        );

        BranchCreatedNotificationEvent event = event(UUID.randomUUID());
        Mockito.when(objectMapper.readValue(Mockito.anyString(), Mockito.eq(BranchCreatedNotificationEvent.class)))
                .thenReturn(event);
        Mockito.when(processedEventRepository.existsById(event.eventId())).thenReturn(true);

        consumer.consume("{\"eventId\":\"" + event.eventId() + "\"}", event.eventId().toString(), "hr-new-branch-notifications");

        Mockito.verify(hrEmailService, Mockito.never()).sendNewBranchEmail(Mockito.any());
        Mockito.verify(processedEventRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void shouldIncreaseFailedNotificationsMetricWhenMessageGoesToDlt() {
        ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
        HrEmailService hrEmailService = Mockito.mock(HrEmailService.class);
        SpringDataProcessedEventRepository processedEventRepository = Mockito.mock(SpringDataProcessedEventRepository.class);
        AppMetrics appMetrics = Mockito.mock(AppMetrics.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        HrBranchNotificationConsumer consumer = new HrBranchNotificationConsumer(
                objectMapper,
                hrEmailService,
                processedEventRepository,
                appMetrics,
                meterRegistry
        );

        consumer.onDlt("{\"eventId\":\"a\"}");
        consumer.onDlt("{\"eventId\":\"b\"}");

        assertEquals(2d, meterRegistry.get("failed_notifications").counter().count());
    }

    private BranchCreatedNotificationEvent event(UUID eventId) {
        return new BranchCreatedNotificationEvent(
                eventId,
                UUID.randomUUID(),
                "BR001",
                "Main Branch",
                "Madrid",
                LocalDateTime.now()
        );
    }
}
