package dev.jotaeme.branchhub.application.security.usecase;

import dev.jotaeme.branchhub.application.common.exception.UnprocessableEntityException;
import dev.jotaeme.branchhub.application.security.model.AuthenticateCommand;
import dev.jotaeme.branchhub.application.security.model.UserAccount;
import dev.jotaeme.branchhub.application.security.port.LoginAttemptPort;
import dev.jotaeme.branchhub.application.security.port.TokenProviderPort;
import dev.jotaeme.branchhub.application.security.port.UserCredentialsPort;
import dev.jotaeme.branchhub.application.security.port.UserDirectoryPort;
import org.junit.jupiter.api.Test;

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
        AuthenticateUserUseCase useCase = new AuthenticateUserUseCase(credentials, tokenProvider, directory, loginAttempts);

        var result = useCase.execute(new AuthenticateCommand("admin", "admin123"));

        assertEquals("token-for-" + ADMIN_ID, result.token());
        assertEquals("refresh-for-" + ADMIN_ID, result.refreshToken());
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
        AuthenticateUserUseCase useCase = new AuthenticateUserUseCase(credentials, tokenProvider, directory, loginAttempts);

        assertThrows(UnprocessableEntityException.class, () -> useCase.execute(new AuthenticateCommand("admin", "wrong")));
        assertEquals("admin", failedFor.get());
    }

    @Test
    void shouldFailWhenCommandIsNull() {
        UserCredentialsPort credentials = (username, password) -> true;
        TokenProviderPort tokenProvider = testTokenProvider();
        UserDirectoryPort directory = testDirectory();
        LoginAttemptPort loginAttempts = noopLoginAttempts();
        AuthenticateUserUseCase useCase = new AuthenticateUserUseCase(credentials, tokenProvider, directory, loginAttempts);

        assertThrows(NullPointerException.class, () -> useCase.execute(null));
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

    private TokenProviderPort testTokenProvider() {
        return new TokenProviderPort() {
            @Override
            public String issueAccessToken(String subject, List<String> roles) {
                return "token-for-" + subject;
            }

            @Override
            public String issueRefreshToken(String subject, List<String> roles) {
                return "refresh-for-" + subject;
            }

            @Override
            public String extractSubjectFromAccessToken(String token) {
                return "admin";
            }

            @Override
            public String extractSubjectFromRefreshToken(String token) {
                return "admin";
            }
        };
    }

    private UserDirectoryPort testDirectory() {
        return new UserDirectoryPort() {
            @Override
            public Optional<UserAccount> findByEmail(String email) {
                if ("admin".equals(email)) {
                    return Optional.of(new UserAccount(ADMIN_ID, List.of("APP_ADMIN")));
                }
                return Optional.empty();
            }

            @Override
            public Optional<UserAccount> findById(UUID id) {
                if (ADMIN_ID.equals(id)) {
                    return Optional.of(new UserAccount(ADMIN_ID, List.of("APP_ADMIN")));
                }
                return Optional.empty();
            }
        };
    }
}
