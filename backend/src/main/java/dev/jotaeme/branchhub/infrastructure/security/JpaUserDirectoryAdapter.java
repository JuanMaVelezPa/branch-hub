package dev.jotaeme.branchhub.infrastructure.security;

import dev.jotaeme.branchhub.application.security.model.UserAccount;
import dev.jotaeme.branchhub.application.security.port.UserDirectoryPort;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamUserJpaEntity;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamUserRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaUserDirectoryAdapter implements UserDirectoryPort {
    private final IamUserRepository users;

    public JpaUserDirectoryAdapter(IamUserRepository users) {
        this.users = users;
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return users.findByEmailIgnoreCase(email.trim()).map(this::toAccount);
    }

    @Override
    public Optional<UserAccount> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return users.findById(id).map(this::toAccount);
    }

    private UserAccount toAccount(IamUserJpaEntity u) {
        List<String> roles = u.getRoles().stream()
                .map(r -> r.getName())
                .sorted()
                .toList();
        return new UserAccount(u.getId(), roles);
    }
}

