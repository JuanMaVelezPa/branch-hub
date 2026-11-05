package dev.jotaeme.branchhub.infrastructure.security;

import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamRoleJpaEntity;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamRoleRepository;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamUserJpaEntity;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class IamBootstrapRunner implements ApplicationRunner {
    private static final List<String> BASE_ROLES = List.of("SUPER_ADMIN", "APP_ADMIN", "USER");

    private final boolean enabled;
    private final String superAdminEmail;
    private final String superAdminPassword;
    private final IamUserRepository users;
    private final IamRoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    public IamBootstrapRunner(
            @Value("${app.security.bootstrap.enabled:true}") boolean enabled,
            @Value("${app.security.bootstrap.super-admin.email:}") String superAdminEmail,
            @Value("${app.security.bootstrap.super-admin.password:}") String superAdminPassword,
            IamUserRepository users,
            IamRoleRepository roles,
            PasswordEncoder passwordEncoder) {
        this.enabled = enabled;
        this.superAdminEmail = superAdminEmail;
        this.superAdminPassword = superAdminPassword;
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        bootstrap();
    }

    @Transactional
    void bootstrap() {
        Instant now = Instant.now();

        for (String roleName : BASE_ROLES) {
            roles.findByName(roleName).orElseGet(() -> roles.save(new IamRoleJpaEntity(UUID.randomUUID(), roleName, now)));
        }

        String email = superAdminEmail == null ? null : superAdminEmail.trim();
        if (email == null || email.isBlank() || superAdminPassword == null || superAdminPassword.isBlank()) {
            return;
        }

        if (users.findByEmailIgnoreCase(email).isPresent()) {
            return;
        }

        IamUserJpaEntity admin = new IamUserJpaEntity(UUID.randomUUID());
        admin.setEmail(email);
        admin.setEmailVerifiedAt(now);
        admin.setEnabled(true);
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        admin.setPasswordHash(passwordEncoder.encode(superAdminPassword));
        admin.addRole(roles.findByName("SUPER_ADMIN").orElseThrow());
        admin.addRole(roles.findByName("APP_ADMIN").orElseThrow());
        users.save(admin);
    }
}

