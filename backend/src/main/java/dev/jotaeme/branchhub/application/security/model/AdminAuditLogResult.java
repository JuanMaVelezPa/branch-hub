package dev.jotaeme.branchhub.application.security.model;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditLogResult(
        UUID id,
        UUID actorUserId,
        String action,
        UUID targetUserId,
        String metadata,
        Instant createdAt) {
}
