package com.jm.spring_web.application.security.usecase;

import com.jm.spring_web.application.common.exception.UnauthorizedException;
import com.jm.spring_web.application.security.model.AuthResult;
import com.jm.spring_web.application.security.model.RefreshTokenCommand;
import com.jm.spring_web.application.security.model.RefreshRotationOutcome;
import com.jm.spring_web.application.security.port.RefreshSessionPort;
import com.jm.spring_web.application.security.port.TokenProviderPort;
import com.jm.spring_web.application.security.port.UserDirectoryPort;

import java.util.Objects;

public class RefreshTokenUseCase {
    private final TokenProviderPort tokenProviderPort;
    private final UserDirectoryPort userDirectoryPort;
    private final RefreshSessionPort refreshSessionPort;

    public RefreshTokenUseCase(
            TokenProviderPort tokenProviderPort,
            UserDirectoryPort userDirectoryPort,
            RefreshSessionPort refreshSessionPort) {
        this.tokenProviderPort = Objects.requireNonNull(tokenProviderPort);
        this.userDirectoryPort = Objects.requireNonNull(userDirectoryPort);
        this.refreshSessionPort = Objects.requireNonNull(refreshSessionPort);
    }

    public AuthResult execute(RefreshTokenCommand command) {
        if (command == null || command.refreshToken() == null || command.refreshToken().isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        try {
            RefreshRotationOutcome outcome = refreshSessionPort.rotate(command.refreshToken())
                    .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));
            var account = userDirectoryPort.findById(outcome.userId())
                    .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));
            String subject = outcome.userId().toString();
            return new AuthResult(
                    tokenProviderPort.issueAccessToken(subject, account.roles()),
                    outcome.newRawRefreshToken(),
                    null);
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
    }
}
