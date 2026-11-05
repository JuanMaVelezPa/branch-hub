package dev.jotaeme.branchhub.application.notification.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record BranchCreatedNotificationEvent(
        UUID eventId,
        UUID branchId,
        String code,
        String name,
        String city,
        LocalDateTime createdAt
) {
}
