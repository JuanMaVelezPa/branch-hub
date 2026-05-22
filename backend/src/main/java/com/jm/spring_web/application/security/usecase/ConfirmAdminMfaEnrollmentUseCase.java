package com.jm.spring_web.application.security.usecase;

import com.jm.spring_web.application.common.exception.ConflictException;
import com.jm.spring_web.application.common.exception.NotFoundException;
import com.jm.spring_web.application.common.exception.UnprocessableEntityException;
import com.jm.spring_web.application.security.AdminMfaPolicy;
import com.jm.spring_web.application.security.port.AdminMfaPort;
import com.jm.spring_web.application.security.port.TotpVerificationPort;
import com.jm.spring_web.application.security.port.UserDirectoryPort;

import java.util.Objects;
import java.util.UUID;

public class ConfirmAdminMfaEnrollmentUseCase {
    private final UserDirectoryPort userDirectoryPort;
    private final AdminMfaPort adminMfaPort;
    private final TotpVerificationPort totpVerificationPort;

    public ConfirmAdminMfaEnrollmentUseCase(
            UserDirectoryPort userDirectoryPort,
            AdminMfaPort adminMfaPort,
            TotpVerificationPort totpVerificationPort) {
        this.userDirectoryPort = Objects.requireNonNull(userDirectoryPort);
        this.adminMfaPort = Objects.requireNonNull(adminMfaPort);
        this.totpVerificationPort = Objects.requireNonNull(totpVerificationPort);
    }

    public void execute(UUID userId, String code) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        var account = userDirectoryPort.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (!AdminMfaPolicy.isPrivilegedAdmin(account.roles())) {
            throw new UnprocessableEntityException("MFA enrollment is only available for administrator accounts");
        }
        if (account.mfaEnabled()) {
            throw new ConflictException("MFA is already enabled for this account");
        }
        byte[] pending = adminMfaPort.findPendingSecretPlain(userId)
                .orElseThrow(() -> new UnprocessableEntityException("No MFA enrollment in progress; call enroll/begin first"));
        if (!totpVerificationPort.verify(pending, code)) {
            throw new UnprocessableEntityException("Invalid authenticator code");
        }
        adminMfaPort.activatePendingSecret(userId);
    }
}
