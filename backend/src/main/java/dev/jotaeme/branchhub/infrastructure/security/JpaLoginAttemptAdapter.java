package dev.jotaeme.branchhub.infrastructure.security;

import dev.jotaeme.branchhub.application.security.port.LoginAttemptPort;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamUserJpaEntity;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamUserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class JpaLoginAttemptAdapter implements LoginAttemptPort {
    private final IamUserRepository users;
    private final LoginLockoutProperties lockoutProperties;

    public JpaLoginAttemptAdapter(IamUserRepository users, LoginLockoutProperties lockoutProperties) {
        this.users = users;
        this.lockoutProperties = lockoutProperties;
    }

    @Override
    @Transactional
    public void onFailedLogin(String attemptedEmail) {
        if (attemptedEmail == null || attemptedEmail.isBlank()) {
            return;
        }
        users.findByEmailIgnoreCase(attemptedEmail.trim()).ifPresent(this::applyFailure);
    }

    @Override
    @Transactional
    public void onSuccessfulLogin(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        users.findByEmailIgnoreCase(email.trim()).ifPresent(this::clearFailures);
    }

    private void applyFailure(IamUserJpaEntity user) {
        Instant now = Instant.now();
        Instant lockedUntil = user.getLockedUntil();
        if (lockedUntil != null && lockedUntil.isAfter(now)) {
            return;
        }
        if (lockedUntil != null) {
            user.setFailedLoginCount(0);
            user.setLockedUntil(null);
        }
        int next = user.getFailedLoginCount() + 1;
        user.setFailedLoginCount(next);
        user.setUpdatedAt(now);
        int max = Math.max(1, lockoutProperties.getMaxFailedAttempts());
        if (next >= max) {
            int minutes = Math.max(1, lockoutProperties.getLockoutMinutes());
            user.setLockedUntil(now.plus(minutes, ChronoUnit.MINUTES));
        }
        users.save(user);
    }

    private void clearFailures(IamUserJpaEntity user) {
        Instant now = Instant.now();
        if (user.getFailedLoginCount() == 0 && user.getLockedUntil() == null) {
            return;
        }
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setUpdatedAt(now);
        users.save(user);
    }
}
