package dev.jotaeme.branchhub.entrypoints.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditLogResponse(
        UUID id,
        UUID actorUserId,
        String action,
        UUID targetUserId,
        String metadata,
        Instant createdAt) {
}
