package com.jm.spring_web.infrastructure.persistence.iam;

import com.jm.spring_web.application.common.exception.NotFoundException;
import com.jm.spring_web.application.security.port.AdminMfaPort;
import com.jm.spring_web.infrastructure.security.MfaSecretCipher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaAdminMfaAdapter implements AdminMfaPort {
    private final IamUserRepository users;
    private final MfaSecretCipher cipher;

    public JpaAdminMfaAdapter(IamUserRepository users, MfaSecretCipher cipher) {
        this.users = users;
        this.cipher = cipher;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<byte[]> findActiveSecretPlain(UUID userId) {
        return users.findById(userId)
                .filter(IamUserJpaEntity::isMfaEnabled)
                .map(IamUserJpaEntity::getMfaSecretEncrypted)
                .filter(s -> s != null && !s.isBlank())
                .map(cipher::decrypt);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<byte[]> findPendingSecretPlain(UUID userId) {
        return users.findById(userId)
                .map(IamUserJpaEntity::getMfaPendingSecretEncrypted)
                .filter(s -> s != null && !s.isBlank())
                .map(cipher::decrypt);
    }

    @Override
    @Transactional
    public void storePendingSecret(UUID userId, byte[] secretPlain) {
        IamUserJpaEntity u = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        u.setMfaPendingSecretEncrypted(cipher.encrypt(secretPlain));
        u.setUpdatedAt(Instant.now());
        users.save(u);
    }

    @Override
    @Transactional
    public void activatePendingSecret(UUID userId) {
        IamUserJpaEntity u = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        String pending = u.getMfaPendingSecretEncrypted();
        if (pending == null || pending.isBlank()) {
            throw new IllegalStateException("No pending MFA secret");
        }
        u.setMfaSecretEncrypted(pending);
        u.setMfaPendingSecretEncrypted(null);
        u.setMfaEnabled(true);
        u.setUpdatedAt(Instant.now());
        users.save(u);
    }
}
