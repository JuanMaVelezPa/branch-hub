package dev.jotaeme.branchhub.infrastructure.persistence;

import dev.jotaeme.branchhub.application.notification.model.OutboxEventCommand;
import dev.jotaeme.branchhub.application.notification.port.OutboxEventRepositoryPort;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventRepositoryAdapter implements OutboxEventRepositoryPort {
    private final SpringDataOutboxEventRepository repository;

    public OutboxEventRepositoryAdapter(SpringDataOutboxEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void savePending(OutboxEventCommand command) {
        OutboxEventJpaEntity entity = OutboxEventJpaEntity.builder()
                .id(command.eventId())
                .aggregateType(command.aggregateType())
                .aggregateId(command.aggregateId())
                .eventType(command.eventType())
                .payload(command.payload())
                .status(OutboxEventStatus.PENDING)
                .retryCount(0)
                .createdAt(command.createdAt())
                .build();
        repository.save(entity);
    }
}
