package com.jm.spring_web.application.security.usecase;

import com.jm.spring_web.application.common.exception.ConflictException;
import com.jm.spring_web.application.common.exception.NotFoundException;
import com.jm.spring_web.application.common.exception.UnprocessableEntityException;
import com.jm.spring_web.application.security.AdminMfaPolicy;
import com.jm.spring_web.application.security.OtpAuthUriBuilder;
import com.jm.spring_web.application.security.model.MfaEnrollBeginResult;
import com.jm.spring_web.application.security.port.AdminMfaPort;
import com.jm.spring_web.application.security.port.UserDirectoryPort;
import com.jm.spring_web.application.security.totp.Base32Encoding;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.UUID;

public class BeginAdminMfaEnrollmentUseCase {
    private final UserDirectoryPort userDirectoryPort;
    private final AdminMfaPort adminMfaPort;
    private final String issuer;
    private final SecureRandom secureRandom = new SecureRandom();

    public BeginAdminMfaEnrollmentUseCase(
            UserDirectoryPort userDirectoryPort,
            AdminMfaPort adminMfaPort,
            String issuer) {
        this.userDirectoryPort = Objects.requireNonNull(userDirectoryPort);
        this.adminMfaPort = Objects.requireNonNull(adminMfaPort);
        this.issuer = issuer == null || issuer.isBlank() ? "spring-web" : issuer.trim();
    }

    public MfaEnrollBeginResult execute(UUID userId) {
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
        byte[] secret = new byte[20];
        secureRandom.nextBytes(secret);
        adminMfaPort.storePendingSecret(userId, secret);
        String base32 = Base32Encoding.encode(secret);
        String label = account.email() != null && !account.email().isBlank() ? account.email() : userId.toString();
        String uri = OtpAuthUriBuilder.totp(issuer, label, base32);
        return new MfaEnrollBeginResult(uri, base32);
    }
}
