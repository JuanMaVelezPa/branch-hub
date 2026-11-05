package dev.jotaeme.branchhub.application.security.usecase;

import dev.jotaeme.branchhub.application.security.model.AdminUserResult;
import dev.jotaeme.branchhub.application.security.port.AdminUserPort;
import dev.jotaeme.branchhub.application.security.port.IamAuditPort;

import java.util.Objects;
import java.util.UUID;

public class AdminSetUserEnabledUseCase {
    private final AdminUserPort adminUserPort;
    private final IamAuditPort auditPort;

    public AdminSetUserEnabledUseCase(AdminUserPort adminUserPort, IamAuditPort auditPort) {
        this.adminUserPort = Objects.requireNonNull(adminUserPort);
        this.auditPort = Objects.requireNonNull(auditPort);
    }

    public AdminUserResult execute(UUID actorUserId, UUID targetUserId, boolean enabled) {
        if (actorUserId == null) {
            throw new IllegalArgumentException("actorUserId is required");
        }
        if (targetUserId == null) {
            throw new IllegalArgumentException("targetUserId is required");
        }
        if (!enabled && actorUserId.equals(targetUserId)) {
            throw new dev.jotaeme.branchhub.application.common.exception.UnprocessableEntityException("You cannot disable your own account");
        }
        AdminUserResult updated = adminUserPort.setEnabled(targetUserId, enabled);
        auditPort.record(actorUserId, enabled ? "iam.user.enable" : "iam.user.disable", targetUserId, null);
        return updated;
    }
}

