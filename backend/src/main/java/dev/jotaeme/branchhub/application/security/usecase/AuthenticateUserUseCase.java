package dev.jotaeme.branchhub.application.security.usecase;

import dev.jotaeme.branchhub.application.common.exception.UnprocessableEntityException;
import dev.jotaeme.branchhub.application.security.model.AuthResult;
import dev.jotaeme.branchhub.application.security.model.AuthenticateCommand;
import dev.jotaeme.branchhub.application.security.model.UserAccount;
import dev.jotaeme.branchhub.application.security.port.LoginAttemptPort;
import dev.jotaeme.branchhub.application.security.port.TokenProviderPort;
import dev.jotaeme.branchhub.application.security.port.UserCredentialsPort;
import dev.jotaeme.branchhub.application.security.port.UserDirectoryPort;

import java.util.Objects;

public class AuthenticateUserUseCase {
    private final UserCredentialsPort userCredentialsPort;
    private final TokenProviderPort tokenProviderPort;
    private final UserDirectoryPort userDirectoryPort;
    private final LoginAttemptPort loginAttemptPort;

    public AuthenticateUserUseCase(
            UserCredentialsPort userCredentialsPort,
            TokenProviderPort tokenProviderPort,
            UserDirectoryPort userDirectoryPort,
            LoginAttemptPort loginAttemptPort) {
        this.userCredentialsPort = Objects.requireNonNull(userCredentialsPort);
        this.tokenProviderPort = Objects.requireNonNull(tokenProviderPort);
        this.userDirectoryPort = Objects.requireNonNull(userDirectoryPort);
        this.loginAttemptPort = Objects.requireNonNull(loginAttemptPort);
    }

    public AuthResult execute(AuthenticateCommand command) {
        if (!userCredentialsPort.authenticate(command.username(), command.password())) {
            loginAttemptPort.onFailedLogin(command.username());
            throw new UnprocessableEntityException("Invalid credentials");
        }
        UserAccount account = userDirectoryPort.findByEmail(command.username())
                .orElseThrow(() -> new UnprocessableEntityException("Invalid credentials"));
        loginAttemptPort.onSuccessfulLogin(command.username());
        return new AuthResult(
                tokenProviderPort.issueAccessToken(account.id().toString(), account.roles()),
                tokenProviderPort.issueRefreshToken(account.id().toString(), account.roles()));
    }
}
