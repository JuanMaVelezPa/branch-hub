package dev.jotaeme.branchhub.infrastructure.security;

import dev.jotaeme.branchhub.infrastructure.demo.DemoProperties;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamRoleRepository;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamUserJpaEntity;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Creates the public demo user (APP_ADMIN only) at startup when {@code app.demo.enabled=true} and
 * no account with the demo email exists. Runs after {@link IamBootstrapRunner}, which seeds the
 * roles. An existing account is left as it is; the scheduled reset restores it.
 */
@Component
@Order(IamBootstrapRunner.ORDER + 1)
public class DemoUserBootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoUserBootstrapRunner.class);

    private final DemoProperties demo;
    private final IamUserRepository users;
    private final IamRoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    public DemoUserBootstrapRunner(
            DemoProperties demo,
            IamUserRepository users,
            IamRoleRepository roles,
            PasswordEncoder passwordEncoder) {
        this.demo = demo;
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!demo.enabled()) {
            return;
        }
        String email = demo.user().email();
        if (users.findByEmailIgnoreCase(email).isPresent()) {
            return;
        }

        var appAdmin = roles.findByName(DemoProperties.DEMO_ROLE)
                .orElseThrow(() -> new IllegalStateException(
                        "Role " + DemoProperties.DEMO_ROLE + " not found; the IAM bootstrap must seed roles before the demo user"));

        Instant now = Instant.now();
        IamUserJpaEntity user = new IamUserJpaEntity(UUID.randomUUID());
        user.setEmail(email);
        user.setEmailVerifiedAt(now);
        user.setEnabled(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setPasswordHash(passwordEncoder.encode(demo.user().password()));
        user.addRole(appAdmin);
        users.save(user);
        log.info("Demo user created: email={}, roles=[{}]", email, DemoProperties.DEMO_ROLE);
    }
}
