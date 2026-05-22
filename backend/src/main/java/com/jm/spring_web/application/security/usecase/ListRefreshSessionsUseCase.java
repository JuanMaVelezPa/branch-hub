package com.jm.spring_web.application.security.usecase;

import com.jm.spring_web.application.security.model.RefreshSessionListItem;
import com.jm.spring_web.application.security.model.RefreshSessionSummary;
import com.jm.spring_web.application.security.port.RefreshSessionPort;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ListRefreshSessionsUseCase {

    private final RefreshSessionPort refreshSessionPort;

    public ListRefreshSessionsUseCase(RefreshSessionPort refreshSessionPort) {
        this.refreshSessionPort = Objects.requireNonNull(refreshSessionPort);
    }

    public List<RefreshSessionListItem> execute(UUID userId, Optional<String> rawRefreshFromCookie) {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(rawRefreshFromCookie);
        Optional<UUID> currentId = rawRefreshFromCookie.flatMap(
                raw -> refreshSessionPort.resolveActiveSessionForUser(userId, raw).map(RefreshSessionSummary::id));
        return refreshSessionPort.listActiveSessions(userId).stream()
                .map(s -> new RefreshSessionListItem(
                        s.id(),
                        s.familyId(),
                        s.createdAt(),
                        s.expiresAt(),
                        currentId.filter(id -> id.equals(s.id())).isPresent()))
                .toList();
    }
}
