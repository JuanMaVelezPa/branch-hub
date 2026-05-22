package com.jm.spring_web.application.security.usecase;

import com.jm.spring_web.application.security.port.RefreshSessionPort;

import java.util.Objects;
import java.util.UUID;

public class RevokeRefreshSessionUseCase {

    private final RefreshSessionPort refreshSessionPort;

    public RevokeRefreshSessionUseCase(RefreshSessionPort refreshSessionPort) {
        this.refreshSessionPort = Objects.requireNonNull(refreshSessionPort);
    }

    public void execute(UUID userId, UUID sessionId) {
        refreshSessionPort.revokeSessionForUser(Objects.requireNonNull(userId), Objects.requireNonNull(sessionId));
    }
}
