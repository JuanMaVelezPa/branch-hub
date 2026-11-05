package dev.jotaeme.branchhub.application.security.usecase;

import dev.jotaeme.branchhub.application.common.exception.ConflictException;
import dev.jotaeme.branchhub.application.security.model.AdminUserResult;
import dev.jotaeme.branchhub.application.security.model.CreateUserCommand;
import dev.jotaeme.branchhub.application.security.port.AdminUserPort;
import dev.jotaeme.branchhub.application.security.port.IamAuditPort;

import java.util.Objects;
import java.util.UUID;

public class AdminCreateUserUseCase {
    private final AdminUserPort adminUserPort;
    private final IamAuditPort auditPort;

    public AdminCreateUserUseCase(AdminUserPort adminUserPort, IamAuditPort auditPort) {
        this.adminUserPort = Objects.requireNonNull(adminUserPort);
        this.auditPort = Objects.requireNonNull(auditPort);
    }

    public AdminUserResult execute(UUID actorUserId, CreateUserCommand command) {
        if (actorUserId == null) {
            throw new IllegalArgumentException("actorUserId is required");
        }
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        try {
            AdminUserResult created = adminUserPort.create(command);
            auditPort.record(actorUserId, "iam.user.create", created.id(), "email=" + created.email());
            return created;
        } catch (RuntimeException ex) {
            // Bubble up ConflictException etc. unchanged.
            throw ex;
        }
    }
}

