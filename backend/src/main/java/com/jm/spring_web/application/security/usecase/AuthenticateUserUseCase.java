package com.jm.spring_web.application.security.usecase;

import com.jm.spring_web.application.common.exception.UnprocessableEntityException;
import com.jm.spring_web.application.security.AdminMfaPolicy;
import com.jm.spring_web.application.security.model.AuthResult;
import com.jm.spring_web.application.security.model.AuthenticateCommand;
import com.jm.spring_web.application.security.model.UserAccount;
import com.jm.spring_web.application.security.port.LoginAttemptPort;
import com.jm.spring_web.application.security.port.RefreshSessionPort;
import com.jm.spring_web.application.security.port.TokenProviderPort;
import com.jm.spring_web.application.security.port.UserCredentialsPort;
import com.jm.spring_web.application.security.port.UserDirectoryPort;

import java.util.Objects;

public class AuthenticateUserUseCase {
    private final UserCredentialsPort userCredentialsPort;
    private final TokenProviderPort tokenProviderPort;
    private final UserDirectoryPort userDirectoryPort;
    private final LoginAttemptPort loginAttemptPort;
    private final RefreshSessionPort refreshSessionPort;

    public AuthenticateUserUseCase(
            UserCredentialsPort userCredentialsPort,
            TokenProviderPort tokenProviderPort,
            UserDirectoryPort userDirectoryPort,
            LoginAttemptPort loginAttemptPort,
            RefreshSessionPort refreshSessionPort) {
        this.userCredentialsPort = Objects.requireNonNull(userCredentialsPort);
        this.tokenProviderPort = Objects.requireNonNull(tokenProviderPort);
        this.userDirectoryPort = Objects.requireNonNull(userDirectoryPort);
        this.loginAttemptPort = Objects.requireNonNull(loginAttemptPort);
        this.refreshSessionPort = Objects.requireNonNull(refreshSessionPort);
    }

    public AuthResult execute(AuthenticateCommand command) {
        if (!userCredentialsPort.authenticate(command.username(), command.password())) {
            loginAttemptPort.onFailedLogin(command.username());
            throw new UnprocessableEntityException("Invalid credentials");
        }
        UserAccount account = userDirectoryPort.findByEmail(command.username())
                .orElseThrow(() -> new UnprocessableEntityException("Invalid credentials"));
        loginAttemptPort.onSuccessfulLogin(command.username());
        if (AdminMfaPolicy.requiresSecondFactor(account)) {
            return new AuthResult(
                    null,
                    null,
                    tokenProviderPort.issueMfaChallengeToken(account.id().toString()));
        }
        return new AuthResult(
                tokenProviderPort.issueAccessToken(account.id().toString(), account.roles()),
                refreshSessionPort.createSession(account.id()),
                null);
    }
}
