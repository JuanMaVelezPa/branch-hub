package dev.jotaeme.branchhub.application.security.usecase;

import dev.jotaeme.branchhub.application.common.exception.UnauthorizedException;
import dev.jotaeme.branchhub.application.security.model.AuthResult;
import dev.jotaeme.branchhub.application.security.model.RefreshTokenCommand;
import dev.jotaeme.branchhub.application.security.port.TokenProviderPort;
import dev.jotaeme.branchhub.application.security.port.UserDirectoryPort;

import java.util.UUID;
import java.util.Objects;

public class RefreshTokenUseCase {
    private final TokenProviderPort tokenProviderPort;
    private final UserDirectoryPort userDirectoryPort;

    public RefreshTokenUseCase(TokenProviderPort tokenProviderPort, UserDirectoryPort userDirectoryPort) {
        this.tokenProviderPort = Objects.requireNonNull(tokenProviderPort);
        this.userDirectoryPort = Objects.requireNonNull(userDirectoryPort);
    }

    public AuthResult execute(RefreshTokenCommand command) {
        if (command == null || command.refreshToken() == null || command.refreshToken().isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        try {
            String subject = tokenProviderPort.extractSubjectFromRefreshToken(command.refreshToken());
            UUID userId = UUID.fromString(subject);
            var account = userDirectoryPort.findById(userId)
                    .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));
            return new AuthResult(
                    tokenProviderPort.issueAccessToken(subject, account.roles()),
                    tokenProviderPort.issueRefreshToken(subject, account.roles()));
        } catch (RuntimeException ex) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
    }
}
