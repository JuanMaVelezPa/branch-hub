package dev.jotaeme.branchhub.application.security.port;

import java.util.UUID;

public interface IamAuditPort {
    void record(UUID actorUserId, String action, UUID targetUserId, String metadata);
}

