package com.jm.spring_web.entrypoints.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record RefreshSessionResponse(
        UUID id, UUID familyId, Instant createdAt, Instant expiresAt, boolean current) {
}
