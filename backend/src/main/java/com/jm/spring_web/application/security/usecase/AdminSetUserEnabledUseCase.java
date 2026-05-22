package com.jm.spring_web.application.security.usecase;

import com.jm.spring_web.application.email.EmailTemplateIds;
import com.jm.spring_web.application.email.model.EmailEnqueueCommand;
import com.jm.spring_web.application.email.port.EmailOutboxPort;
import com.jm.spring_web.application.security.model.AdminSetEnabledOutcome;
import com.jm.spring_web.application.security.model.AdminUserResult;
import com.jm.spring_web.application.security.port.AdminUserPort;
import com.jm.spring_web.application.security.port.IamAuditPort;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class AdminSetUserEnabledUseCase {
    private final AdminUserPort adminUserPort;
    private final IamAuditPort auditPort;
    private final EmailOutboxPort emailOutboxPort;

    public AdminSetUserEnabledUseCase(
            AdminUserPort adminUserPort,
            IamAuditPort auditPort,
            EmailOutboxPort emailOutboxPort) {
        this.adminUserPort = Objects.requireNonNull(adminUserPort);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.emailOutboxPort = Objects.requireNonNull(emailOutboxPort);
    }

    public AdminUserResult execute(UUID actorUserId, UUID targetUserId, boolean enabled) {
        if (actorUserId == null) {
            throw new IllegalArgumentException("actorUserId is required");
        }
        if (targetUserId == null) {
            throw new IllegalArgumentException("targetUserId is required");
        }
        if (!enabled && actorUserId.equals(targetUserId)) {
            throw new com.jm.spring_web.application.common.exception.UnprocessableEntityException("You cannot disable your own account");
        }
        AdminSetEnabledOutcome outcome = adminUserPort.setEnabled(targetUserId, enabled);
        if (outcome.becameActive()) {
            AdminUserResult u = outcome.user();
            emailOutboxPort.enqueue(new EmailEnqueueCommand(
                    EmailTemplateIds.ACCOUNT_ACTIVATED,
                    u.email(),
                    null,
                    Map.of(
                            "title", "Your account is active",
                            "lead", "An administrator has enabled your account. You can sign in again.",
                            "detail",
                            "If you did not expect this message, contact your administrator immediately.")));
        }
        auditPort.record(actorUserId, enabled ? "iam.user.enable" : "iam.user.disable", targetUserId, null);
        return outcome.user();
    }
}

