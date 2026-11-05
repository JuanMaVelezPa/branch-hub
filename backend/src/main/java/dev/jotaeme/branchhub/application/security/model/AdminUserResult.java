package dev.jotaeme.branchhub.application.security.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserResult(
        UUID id,
        String email,
        boolean enabled,
        Instant lockedUntil,
        Instant createdAt,
        List<String> roles
) {
}

