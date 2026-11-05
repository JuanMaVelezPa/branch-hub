package dev.jotaeme.branchhub.application.security.usecase;

import dev.jotaeme.branchhub.application.common.exception.UnauthorizedException;
import dev.jotaeme.branchhub.application.security.model.RefreshTokenCommand;
import dev.jotaeme.branchhub.application.security.model.UserAccount;
import dev.jotaeme.branchhub.application.security.port.TokenProviderPort;
import dev.jotaeme.branchhub.application.security.port.UserDirectoryPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefreshTokenUseCaseTest {
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void shouldIssueNewTokensWhenRefreshTokenIsValid() {
        RefreshTokenUseCase useCase = new RefreshTokenUseCase(tokenProvider(), directory());

        var result = useCase.execute(new RefreshTokenCommand("refresh-token"));

        assertEquals("new-access-token", result.token());
        assertEquals("new-refresh-token", result.refreshToken());
    }

    @Test
    void shouldFailWhenRefreshTokenIsMissing() {
        RefreshTokenUseCase useCase = new RefreshTokenUseCase(tokenProvider(), directory());

        assertThrows(UnauthorizedException.class, () -> useCase.execute(new RefreshTokenCommand("")));
    }

    @Test
    void shouldFailWhenRefreshTokenIsInvalid() {
        TokenProviderPort tokenProvider = new TokenProviderPort() {
            @Override
            public String issueAccessToken(String subject, List<String> roles) {
                return "new-access-token";
            }

            @Override
            public String issueRefreshToken(String subject, List<String> roles) {
                return "new-refresh-token";
            }

            @Override
            public String extractSubjectFromAccessToken(String token) {
                return "admin";
            }

            @Override
            public String extractSubjectFromRefreshToken(String token) {
                throw new RuntimeException("bad token");
            }
        };
        RefreshTokenUseCase useCase = new RefreshTokenUseCase(tokenProvider, directory());

        assertThrows(UnauthorizedException.class, () -> useCase.execute(new RefreshTokenCommand("bad-token")));
    }

    private TokenProviderPort tokenProvider() {
        return new TokenProviderPort() {
            @Override
            public String issueAccessToken(String subject, List<String> roles) {
                return "new-access-token";
            }

            @Override
            public String issueRefreshToken(String subject, List<String> roles) {
                return "new-refresh-token";
            }

            @Override
            public String extractSubjectFromAccessToken(String token) {
                return "admin";
            }

            @Override
            public String extractSubjectFromRefreshToken(String token) {
                return USER_ID.toString();
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
                    return Optional.of(new UserAccount(USER_ID, List.of("APP_ADMIN")));
                }
                return Optional.empty();
            }
        };
    }
}
