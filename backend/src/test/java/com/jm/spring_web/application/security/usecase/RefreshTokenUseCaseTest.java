package com.jm.spring_web.application.security.usecase;

import com.jm.spring_web.application.common.exception.UnauthorizedException;
import com.jm.spring_web.application.security.model.RefreshRotationOutcome;
import com.jm.spring_web.application.security.model.RefreshSessionSummary;
import com.jm.spring_web.application.security.model.RefreshTokenCommand;
import com.jm.spring_web.application.security.model.UserAccount;
import com.jm.spring_web.application.security.port.RefreshSessionPort;
import com.jm.spring_web.application.security.port.TokenProviderPort;
import com.jm.spring_web.application.security.port.UserDirectoryPort;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefreshTokenUseCaseTest {
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void shouldIssueNewTokensWhenRefreshTokenIsValid() {
        RefreshSessionPort sessions = new RefreshSessionPort() {
            @Override
            public String createSession(UUID userId) {
                return "";
            }

            @Override
            public Optional<RefreshRotationOutcome> rotate(String presentedRawToken) {
                if ("refresh-token".equals(presentedRawToken)) {
                    return Optional.of(new RefreshRotationOutcome(USER_ID, "new-refresh-token"));
                }
                return Optional.empty();
            }

            @Override
            public List<RefreshSessionSummary> listActiveSessions(UUID userId) {
                return Collections.emptyList();
            }

            @Override
            public void revokeSessionForUser(UUID userId, UUID sessionId) {
            }

            @Override
            public int revokeAllActiveSessionsExceptFamily(UUID userId, UUID keepFamilyId) {
                return 0;
            }

            @Override
            public Optional<RefreshSessionSummary> resolveActiveSessionForUser(UUID userId, String rawRefreshToken) {
                return Optional.empty();
            }

            @Override
            public void revokePresentedTokenIfActive(String rawRefreshToken) {
            }
        };
        RefreshTokenUseCase useCase = new RefreshTokenUseCase(tokenProvider(), directory(), sessions);

        var result = useCase.execute(new RefreshTokenCommand("refresh-token"));

        assertEquals("new-access-token", result.token());
        assertEquals("new-refresh-token", result.refreshToken());
    }

    @Test
    void shouldFailWhenRefreshTokenIsMissing() {
        RefreshSessionPort sessions = emptySessions();
        RefreshTokenUseCase useCase = new RefreshTokenUseCase(tokenProvider(), directory(), sessions);

        assertThrows(UnauthorizedException.class, () -> useCase.execute(new RefreshTokenCommand("")));
    }

    @Test
    void shouldFailWhenRefreshTokenIsInvalid() {
        RefreshSessionPort sessions = emptySessions();
        RefreshTokenUseCase useCase = new RefreshTokenUseCase(tokenProvider(), directory(), sessions);

        assertThrows(UnauthorizedException.class, () -> useCase.execute(new RefreshTokenCommand("bad-token")));
    }

    private static RefreshSessionPort emptySessions() {
        return new RefreshSessionPort() {
            @Override
            public String createSession(UUID userId) {
                return "";
            }

            @Override
            public Optional<RefreshRotationOutcome> rotate(String presentedRawToken) {
                return Optional.empty();
            }

            @Override
            public List<RefreshSessionSummary> listActiveSessions(UUID userId) {
                return Collections.emptyList();
            }

            @Override
            public void revokeSessionForUser(UUID userId, UUID sessionId) {
            }

            @Override
            public int revokeAllActiveSessionsExceptFamily(UUID userId, UUID keepFamilyId) {
                return 0;
            }

            @Override
            public Optional<RefreshSessionSummary> resolveActiveSessionForUser(UUID userId, String rawRefreshToken) {
                return Optional.empty();
            }

            @Override
            public void revokePresentedTokenIfActive(String rawRefreshToken) {
            }
        };
    }

    private TokenProviderPort tokenProvider() {
        return new TokenProviderPort() {
            @Override
            public String issueAccessToken(String subject, List<String> roles) {
                return "new-access-token";
            }

            @Override
            public String extractSubjectFromAccessToken(String token) {
                return "admin";
            }

            @Override
            public String issueMfaChallengeToken(String subject) {
                return "mfa";
            }

            @Override
            public String extractSubjectFromMfaChallengeToken(String token) {
                return token;
            }
        };
    }

    private UserDirectoryPort directory() {
        return new UserDirectoryPort() {
            @Override
            public Optional<UserAccount> findByEmail(String email) {
                return Optional.empty();
            }

            @Override
            public Optional<UserAccount> findById(UUID id) {
                if (USER_ID.equals(id)) {
                    return Optional.of(new UserAccount(USER_ID, "user@example.com", List.of("APP_ADMIN"), false));
                }
                return Optional.empty();
            }
        };
    }
}
