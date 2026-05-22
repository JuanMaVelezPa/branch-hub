package com.jm.spring_web.application.security.usecase;

import com.jm.spring_web.application.common.exception.UnauthorizedException;
import com.jm.spring_web.application.security.model.RefreshSessionSummary;
import com.jm.spring_web.application.security.port.RefreshSessionPort;

import java.util.Objects;
import java.util.UUID;

public class RevokeOtherRefreshSessionsUseCase {

    private final RefreshSessionPort refreshSessionPort;

    public RevokeOtherRefreshSessionsUseCase(RefreshSessionPort refreshSessionPort) {
        this.refreshSessionPort = Objects.requireNonNull(refreshSessionPort);
    }

    public int execute(UUID userId, String rawRefreshFromCookie) {
        Objects.requireNonNull(userId);
        if (rawRefreshFromCookie == null || rawRefreshFromCookie.isBlank()) {
            throw new UnauthorizedException("Active refresh token required");
        }
        UUID keepFamily = refreshSessionPort
                .resolveActiveSessionForUser(userId, rawRefreshFromCookie)
                .map(RefreshSessionSummary::familyId)
                .orElseThrow(() -> new UnauthorizedException("Active refresh token required"));
        return refreshSessionPort.revokeAllActiveSessionsExceptFamily(userId, keepFamily);
    }
}
