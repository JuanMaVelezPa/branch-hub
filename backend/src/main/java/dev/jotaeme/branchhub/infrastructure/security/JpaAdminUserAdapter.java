package dev.jotaeme.branchhub.infrastructure.security;

import dev.jotaeme.branchhub.application.common.exception.ConflictException;
import dev.jotaeme.branchhub.application.common.exception.NotFoundException;
import dev.jotaeme.branchhub.application.common.exception.UnprocessableEntityException;
import dev.jotaeme.branchhub.application.common.pagination.PageQuery;
import dev.jotaeme.branchhub.application.common.pagination.PageResult;
import dev.jotaeme.branchhub.application.security.EmailPolicy;
import dev.jotaeme.branchhub.application.security.PasswordPolicy;
import dev.jotaeme.branchhub.application.security.model.AdminUserResult;
import dev.jotaeme.branchhub.application.security.model.CreateUserCommand;
import dev.jotaeme.branchhub.application.security.port.AdminUserPort;
import dev.jotaeme.branchhub.infrastructure.persistence.SpringPageRequests;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamRoleJpaEntity;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamRoleRepository;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamUserJpaEntity;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class JpaAdminUserAdapter implements AdminUserPort {
    private final IamUserRepository users;
    private final IamRoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    public JpaAdminUserAdapter(IamUserRepository users, IamRoleRepository roles, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public AdminUserResult create(CreateUserCommand command) {
        String email = normalizeEmail(command.email());
        if (email == null) {
            throw new UnprocessableEntityException("Email is required");
        }
        if (command.rawPassword() == null || command.rawPassword().isBlank()) {
            throw new UnprocessableEntityException("Password is required");
        }
        if (!EmailPolicy.isValid(email)) {
            throw new UnprocessableEntityException("Invalid email format");
        }
        List<String> passwordIssues = PasswordPolicy.violations(command.rawPassword());
        if (!passwordIssues.isEmpty()) {
            throw new UnprocessableEntityException(String.join("; ", passwordIssues));
        }
        if (users.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictException("Email already exists");
        }

        Instant now = Instant.now();
        IamUserJpaEntity u = new IamUserJpaEntity(UUID.randomUUID());
        u.setEmail(email);
        u.setEnabled(true);
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        u.setPasswordHash(passwordEncoder.encode(command.rawPassword()));

        List<String> roleNames = (command.roles() == null || command.roles().isEmpty())
                ? List.of("USER")
                : command.roles();
        for (String roleName : roleNames) {
            u.addRole(resolveRole(roleName));
        }

        IamUserJpaEntity saved = users.save(u);
        return toResult(saved);
    }

    @Override
    public java.util.Optional<AdminUserResult> findById(UUID id) {
        return users.findById(id).map(this::toResult);
    }

    @Override
    public PageResult<AdminUserResult> list(PageQuery query) {
        Page<IamUserJpaEntity> page = users.findAll(SpringPageRequests.from(query));
        return PageResult.of(
                page.getContent().stream().map(this::toResult).toList(),
                page.getTotalElements(),
                query.page(),
                query.size()
        );
    }

    @Override
    @Transactional
    public AdminUserResult setEnabled(UUID userId, boolean enabled) {
        IamUserJpaEntity u = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (!enabled) {
            boolean isSuperAdmin = u.getRoles().stream().anyMatch(r -> "SUPER_ADMIN".equalsIgnoreCase(r.getName()));
            if (isSuperAdmin) {
                throw new UnprocessableEntityException("You cannot disable a SUPER_ADMIN account");
            }
        }
        u.setEnabled(enabled);
        u.setUpdatedAt(Instant.now());
        return toResult(users.save(u));
    }

    @Override
    @Transactional
    public AdminUserResult setRoles(UUID userId, List<String> roleNames) {
        IamUserJpaEntity u = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        u.getRoles().clear();
        for (String roleName : roleNames) {
            u.addRole(resolveRole(roleName));
        }
        u.setUpdatedAt(Instant.now());
        return toResult(users.save(u));
    }

    private IamRoleJpaEntity resolveRole(String roleName) {
        String name = roleName == null ? null : roleName.trim();
        if (name == null || name.isBlank()) {
            throw new UnprocessableEntityException("Role name is required");
        }
        String normalized = name.toUpperCase(Locale.ROOT);
        return roles.findByName(normalized)
                .orElseThrow(() -> new UnprocessableEntityException("Unknown role: " + normalized));
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        return trimmed.isBlank() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private AdminUserResult toResult(IamUserJpaEntity u) {
        List<String> roleNames = u.getRoles().stream().map(IamRoleJpaEntity::getName).sorted().toList();
        return new AdminUserResult(
                u.getId(),
                u.getEmail(),
                u.isEnabled(),
                u.getLockedUntil(),
                u.getCreatedAt(),
                roleNames
        );
    }
}

