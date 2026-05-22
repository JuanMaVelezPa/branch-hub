package com.jm.spring_web.application.security.usecase;

import com.jm.spring_web.application.common.exception.UnprocessableEntityException;
import com.jm.spring_web.application.security.model.AuthenticateCommand;
import com.jm.spring_web.application.security.model.UserAccount;
import com.jm.spring_web.application.security.port.LoginAttemptPort;
import com.jm.spring_web.application.security.model.RefreshRotationOutcome;
import com.jm.spring_web.application.security.model.RefreshSessionSummary;
import com.jm.spring_web.application.security.port.RefreshSessionPort;
import com.jm.spring_web.application.security.port.TokenProviderPort;
import com.jm.spring_web.application.security.port.UserCredentialsPort;
import com.jm.spring_web.application.security.port.UserDirectoryPort;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticateUserUseCaseTest {
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void shouldReturnTokenForValidCredentials() {
        UserCredentialsPort credentials = (username, password) -> "admin".equals(username) && "admin123".equals(password);
        TokenProviderPort tokenProvider = testTokenProvider();
        UserDirectoryPort directory = testDirectory();
        AtomicInteger successes = new AtomicInteger();
        LoginAttemptPort loginAttempts = new LoginAttemptPort() {
            @Override
            public void onFailedLogin(String attemptedEmail) {
            }

            @Override
            public void onSuccessfulLogin(String email) {
                successes.incrementAndGet();
            }
        };
        RefreshSessionPort refreshSession = new RefreshSessionPort() {
            @Override
            public String createSession(UUID userId) {
                return "opaque-refresh-" + userId;
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
        AuthenticateUserUseCase useCase =
                new AuthenticateUserUseCase(credentials, tokenProvider, directory, loginAttempts, refreshSession);

        var result = useCase.execute(new AuthenticateCommand("admin", "admin123"));

        assertEquals("token-for-" + ADMIN_ID, result.token());
        assertEquals("opaque-refresh-" + ADMIN_ID, result.refreshToken());
        assertEquals(1, successes.get());
    }

    @Test
    void shouldFailForInvalidCredentials() {
        UserCredentialsPort credentials = (username, password) -> false;
        TokenProviderPort tokenProvider = testTokenProvider();
        UserDirectoryPort directory = testDirectory();
        AtomicReference<String> failedFor = new AtomicReference<>();
        LoginAttemptPort loginAttempts = new LoginAttemptPort() {
            @Override
            public void onFailedLogin(String attemptedEmail) {
                failedFor.set(attemptedEmail);
            }

            @Override
            public void onSuccessfulLogin(String email) {
            }
        };
        RefreshSessionPort refreshSession = noopRefreshSession();
        AuthenticateUserUseCase useCase =
                new AuthenticateUserUseCase(credentials, tokenProvider, directory, loginAttempts, refreshSession);

        assertThrows(UnprocessableEntityException.class, () -> useCase.execute(new AuthenticateCommand("admin", "wrong")));
        assertEquals("admin", failedFor.get());
    }

    @Test
    void shouldFailWhenCommandIsNull() {
        UserCredentialsPort credentials = (username, password) -> true;
        TokenProviderPort tokenProvider = testTokenProvider();
        UserDirectoryPort directory = testDirectory();
        LoginAttemptPort loginAttempts = noopLoginAttempts();
        RefreshSessionPort refreshSession = noopRefreshSession();
        AuthenticateUserUseCase useCase =
                new AuthenticateUserUseCase(credentials, tokenProvider, directory, loginAttempts, refreshSession);

        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    private static RefreshSessionPort noopRefreshSession() {
        return new RefreshSessionPort() {
            @Override
            public String createSession(UUID userId) {
                return "x";
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

    private static LoginAttemptPort noopLoginAttempts() {
        return new LoginAttemptPort() {
            @Override
            public void onFailedLogin(String attemptedEmail) {
            }

            @Override
            public void onSuccessfulLogin(String email) {
            }
        };
    }

    @Test
    void shouldReturnMfaChallengeWhenAdminHasMfaEnabled() {
        UserCredentialsPort credentials = (username, password) -> "admin".equals(username) && "admin123".equals(password);
        TokenProviderPort tokenProvider = testTokenProvider();
        UserDirectoryPort directory = mfaEnabledAdminDirectory();
        LoginAttemptPort loginAttempts = noopLoginAttempts();
        RefreshSessionPort refreshSession = noopRefreshSession();
        AuthenticateUserUseCase useCase =
                new AuthenticateUserUseCase(credentials, tokenProvider, directory, loginAttempts, refreshSession);

        var result = useCase.execute(new AuthenticateCommand("admin", "admin123"));

        assertEquals("mfa-for-" + ADMIN_ID, result.mfaChallengeToken());
        assertEquals(null, result.token());
        assertEquals(null, result.refreshToken());
    }

    private TokenProviderPort testTokenProvider() {
        return new TokenProviderPort() {
            @Override
            public String issueAccessToken(String subject, List<String> roles) {
                return "token-for-" + subject;
            }

            @Override
            public String extractSubjectFromAccessToken(String token) {
                return "admin";
            }

            @Override
            public String issueMfaChallengeToken(String subject) {
                return "mfa-for-" + subject;
            }

            @Override
            public String extractSubjectFromMfaChallengeToken(String token) {
                return token.replace("mfa-for-", "");
            }
        };
    }

    private UserDirectoryPort testDirectory() {
        return new UserDirectoryPort() {
            @Override
            public Optional<UserAccount> findByEmail(String email) {
                if ("admin".equals(email)) {
                    return Optional.of(new UserAccount(ADMIN_ID, "admin", List.of("APP_ADMIN"), false));
                }
                return Optional.empty();
            }

            @Override
            public Optional<UserAccount> findById(UUID id) {
                if (ADMIN_ID.equals(id)) {
                    return Optional.of(new UserAccount(ADMIN_ID, "admin", List.of("APP_ADMIN"), false));
                }
                return Optional.empty();
            }
        };
    }

    private UserDirectoryPort mfaEnabledAdminDirectory() {
        return new UserDirectoryPort() {
            @Override
            public Optional<UserAccount> findByEmail(String email) {
                if ("admin".equals(email)) {
                    return Optional.of(new UserAccount(ADMIN_ID, "admin", List.of("APP_ADMIN"), true));
                }
                return Optional.empty();
            }

            @Override
            public Optional<UserAccount> findById(UUID id) {
                return Optional.empty();
            }
        };
    }
}
