package dev.jotaeme.branchhub.application.notification.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record OutboxEventCommand(
        UUID eventId,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        String payload,
        LocalDateTime createdAt
) {
    public OutboxEventCommand {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(aggregateType);
        Objects.requireNonNull(aggregateId);
        Objects.requireNonNull(eventType);
        Objects.requireNonNull(payload);
        Objects.requireNonNull(createdAt);
    }
}
