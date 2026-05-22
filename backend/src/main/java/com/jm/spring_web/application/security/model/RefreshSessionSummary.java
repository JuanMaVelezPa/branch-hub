package com.jm.spring_web.application.security.model;

import java.time.Instant;
import java.util.UUID;

public record RefreshSessionSummary(UUID id, UUID familyId, Instant createdAt, Instant expiresAt) {
}
