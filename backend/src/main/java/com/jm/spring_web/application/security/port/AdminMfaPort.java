package com.jm.spring_web.application.security.port;

import java.util.Optional;
import java.util.UUID;

public interface AdminMfaPort {

    Optional<byte[]> findActiveSecretPlain(UUID userId);

    Optional<byte[]> findPendingSecretPlain(UUID userId);

    void storePendingSecret(UUID userId, byte[] secretPlain);

    void activatePendingSecret(UUID userId);
}
