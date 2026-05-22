package com.jm.spring_web.application.security.usecase;

import com.jm.spring_web.application.common.exception.UnprocessableEntityException;
import com.jm.spring_web.application.security.AdminMfaPolicy;
import com.jm.spring_web.application.security.model.AuthResult;
import com.jm.spring_web.application.security.model.VerifyMfaCommand;
import com.jm.spring_web.application.security.port.AdminMfaPort;
import com.jm.spring_web.application.security.port.RefreshSessionPort;
import com.jm.spring_web.application.security.port.TokenProviderPort;
import com.jm.spring_web.application.security.port.TotpVerificationPort;
import com.jm.spring_web.application.security.port.UserDirectoryPort;

import java.util.Objects;
import java.util.UUID;

public class VerifyMfaChallengeUseCase {
    private final TokenProviderPort tokenProviderPort;
    private final UserDirectoryPort userDirectoryPort;
    private final AdminMfaPort adminMfaPort;
    private final TotpVerificationPort totpVerificationPort;
    private final RefreshSessionPort refreshSessionPort;

    public VerifyMfaChallengeUseCase(
            TokenProviderPort tokenProviderPort,
            UserDirectoryPort userDirectoryPort,
            AdminMfaPort adminMfaPort,
            TotpVerificationPort totpVerificationPort,
            RefreshSessionPort refreshSessionPort) {
        this.tokenProviderPort = Objects.requireNonNull(tokenProviderPort);
        this.userDirectoryPort = Objects.requireNonNull(userDirectoryPort);
        this.adminMfaPort = Objects.requireNonNull(adminMfaPort);
        this.totpVerificationPort = Objects.requireNonNull(totpVerificationPort);
        this.refreshSessionPort = Objects.requireNonNull(refreshSessionPort);
    }

    public AuthResult execute(VerifyMfaCommand command) {
        if (command == null || command.mfaChallengeToken() == null || command.mfaChallengeToken().isBlank()) {
            throw new UnprocessableEntityException("MFA challenge token is required");
        }
        if (command.code() == null || command.code().isBlank()) {
            throw new UnprocessableEntityException("Authenticator code is required");
        }
        String subject;
        try {
            subject = tokenProviderPort.extractSubjectFromMfaChallengeToken(command.mfaChallengeToken().trim());
        } catch (RuntimeException ex) {
            throw new UnprocessableEntityException("Invalid MFA challenge token");
        }
        UUID userId;
        try {
            userId = UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new UnprocessableEntityException("Invalid MFA challenge token");
        }
        var account = userDirectoryPort.findById(userId)
                .orElseThrow(() -> new UnprocessableEntityException("Invalid MFA challenge"));
        if (!AdminMfaPolicy.requiresSecondFactor(account)) {
            throw new UnprocessableEntityException("MFA is not active for this account");
        }
        byte[] secret = adminMfaPort.findActiveSecretPlain(userId)
                .orElseThrow(() -> new UnprocessableEntityException("MFA is not configured for this account"));
        if (!totpVerificationPort.verify(secret, command.code())) {
            throw new UnprocessableEntityException("Invalid authenticator code");
        }
        String access = tokenProviderPort.issueAccessToken(userId.toString(), account.roles());
        String refresh = refreshSessionPort.createSession(userId);
        return new AuthResult(access, refresh, null);
    }
}
