package dev.jotaeme.branchhub.application.security.port;

import dev.jotaeme.branchhub.application.security.model.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserDirectoryPort {
    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findById(UUID id);
}

