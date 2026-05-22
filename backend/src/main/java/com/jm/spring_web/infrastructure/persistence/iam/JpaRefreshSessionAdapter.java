package com.jm.spring_web.infrastructure.persistence.iam;

import com.jm.spring_web.application.common.exception.NotFoundException;
import com.jm.spring_web.application.security.model.RefreshRotationOutcome;
import com.jm.spring_web.application.security.model.RefreshSessionSummary;
import com.jm.spring_web.application.security.port.RefreshSessionPort;
import com.jm.spring_web.infrastructure.security.JwtProperties;
import com.jm.spring_web.infrastructure.security.RefreshTokenHasher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaRefreshSessionAdapter implements RefreshSessionPort {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final IamRefreshSessionJpaRepository repository;
    private final JwtProperties jwtProperties;

    public JpaRefreshSessionAdapter(IamRefreshSessionJpaRepository repository, JwtProperties jwtProperties) {
        this.repository = repository;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional
    public String createSession(UUID userId) {
        UUID familyId = UUID.randomUUID();
        return insertNewToken(userId, familyId);
    }

    @Override
    @Transactional
    public Optional<RefreshRotationOutcome> rotate(String presentedRawToken) {
        if (presentedRawToken == null || presentedRawToken.isBlank()) {
            return Optional.empty();
        }
        String hash = RefreshTokenHasher.sha256Hex(presentedRawToken);
        Optional<IamRefreshSessionJpaEntity> rowOpt = repository.findByTokenHash(hash);
        if (rowOpt.isEmpty()) {
            return Optional.empty();
        }
        IamRefreshSessionJpaEntity row = rowOpt.get();
        Instant now = Instant.now();
        if (row.getExpiresAt().isBefore(now)) {
            return Optional.empty();
        }
        if (row.getRevokedAt() != null) {
            revokeFamily(row.getFamilyId(), now);
            return Optional.empty();
        }
        row.setRevokedAt(now);
        repository.save(row);
        String newRaw = insertNewTokenReturningRaw(row.getUserId(), row.getFamilyId(), now);
        return Optional.of(new RefreshRotationOutcome(row.getUserId(), newRaw));
    }

    private String insertNewToken(UUID userId, UUID familyId) {
        return insertNewTokenReturningRaw(userId, familyId, Instant.now());
    }

    private String insertNewTokenReturningRaw(UUID userId, UUID familyId, Instant now) {
        String raw = newRawToken();
        String hash = RefreshTokenHasher.sha256Hex(raw);
        IamRefreshSessionJpaEntity entity = new IamRefreshSessionJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setFamilyId(familyId);
        entity.setTokenHash(hash);
        entity.setExpiresAt(now.plusSeconds(jwtProperties.refreshExpirationSeconds()));
        entity.setCreatedAt(now);
        repository.save(entity);
        return raw;
    }

    private static String newRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void revokeFamily(UUID familyId, Instant now) {
        List<IamRefreshSessionJpaEntity> members = repository.findAllByFamilyId(familyId);
        for (IamRefreshSessionJpaEntity m : members) {
            if (m.getRevokedAt() == null) {
                m.setRevokedAt(now);
                repository.save(m);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefreshSessionSummary> listActiveSessions(UUID userId) {
        Instant now = Instant.now();
        return repository.findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(userId, now).stream()
                .map(JpaRefreshSessionAdapter::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public void revokeSessionForUser(UUID userId, UUID sessionId) {
        IamRefreshSessionJpaEntity row = repository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("Session not found"));
        if (row.getRevokedAt() != null) {
            return;
        }
        Instant now = Instant.now();
        if (!row.getExpiresAt().isBefore(now)) {
            row.setRevokedAt(now);
            repository.save(row);
        }
    }

    @Override
    @Transactional
    public int revokeAllActiveSessionsExceptFamily(UUID userId, UUID keepFamilyId) {
        return repository.revokeActiveInOtherFamilies(userId, keepFamilyId, Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshSessionSummary> resolveActiveSessionForUser(UUID userId, String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return Optional.empty();
        }
        String hash = RefreshTokenHasher.sha256Hex(rawRefreshToken);
        return repository.findByTokenHash(hash)
                .filter(row -> userId.equals(row.getUserId()))
                .filter(row -> row.getRevokedAt() == null)
                .filter(row -> !row.getExpiresAt().isBefore(Instant.now()))
                .map(JpaRefreshSessionAdapter::toSummary);
    }

    @Override
    @Transactional
    public long purgeExpiredBefore(Instant cutoff) {
        return repository.deleteByExpiresAtBefore(cutoff);
    }

    private static RefreshSessionSummary toSummary(IamRefreshSessionJpaEntity row) {
        return new RefreshSessionSummary(row.getId(), row.getFamilyId(), row.getCreatedAt(), row.getExpiresAt());
    }

    @Override
    @Transactional
    public void revokePresentedTokenIfActive(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String hash = RefreshTokenHasher.sha256Hex(rawRefreshToken);
        repository.findByTokenHash(hash).ifPresent(row -> {
            Instant now = Instant.now();
            if (row.getRevokedAt() != null || row.getExpiresAt().isBefore(now)) {
                return;
            }
            row.setRevokedAt(now);
            repository.save(row);
        });
    }
}
