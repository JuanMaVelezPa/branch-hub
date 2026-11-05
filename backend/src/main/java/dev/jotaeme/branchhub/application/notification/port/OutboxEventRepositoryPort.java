package dev.jotaeme.branchhub.application.notification.port;

import dev.jotaeme.branchhub.application.notification.model.OutboxEventCommand;

public interface OutboxEventRepositoryPort {
    void savePending(OutboxEventCommand command);
}
