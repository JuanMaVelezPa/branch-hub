package com.jm.spring_web.application.security.port;

import com.jm.spring_web.application.security.model.RefreshRotationOutcome;
import com.jm.spring_web.application.security.model.RefreshSessionSummary;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionPort {

    /**
     * New login: creates a session family and returns the raw token to store in the HttpOnly cookie.
     */
    String createSession(UUID userId);

    /**
     * Validates the presented token, rotates it (revokes old row, inserts new), or revokes the whole family on reuse.
     */
    Optional<RefreshRotationOutcome> rotate(String presentedRawToken);

    /**
     * Deletes refresh sessions that expired before {@code cutoff}. Safe to run periodically (housekeeping).
     */
    default long purgeExpiredBefore(Instant cutoff) {
        return 0L;
    }

    List<RefreshSessionSummary> listActiveSessions(UUID userId);

    /**
     * Revokes a refresh session row owned by the user. Idempotent if already revoked.
     */
    void revokeSessionForUser(UUID userId, UUID sessionId);

    /**
     * Marks all still-active refresh rows as revoked for this user, except rows in {@code keepFamilyId}.
     *
     * @return rows updated
     */
    int revokeAllActiveSessionsExceptFamily(UUID userId, UUID keepFamilyId);

    /**
     * If the raw token identifies a non-expired, non-revoked session owned by {@code userId}, returns its summary.
     */
    Optional<RefreshSessionSummary> resolveActiveSessionForUser(UUID userId, String rawRefreshToken);

    /**
     * Logout: invalidates the server-side refresh row for this raw token if still active (no JWT required).
     */
    default void revokePresentedTokenIfActive(String rawRefreshToken) {
    }
}
