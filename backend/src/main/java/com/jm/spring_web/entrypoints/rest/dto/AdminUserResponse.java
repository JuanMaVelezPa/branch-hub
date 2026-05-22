package com.jm.spring_web.entrypoints.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        boolean enabled,
        Instant lockedUntil,
        Instant createdAt,
        List<String> roles,
        @Schema(description = "Whether TOTP MFA is active for this account") boolean mfaEnabled
) {
}

