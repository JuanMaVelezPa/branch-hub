package dev.jotaeme.branchhub.application.security.usecase;

import dev.jotaeme.branchhub.application.common.pagination.PageQuery;
import dev.jotaeme.branchhub.application.common.pagination.PageResult;
import dev.jotaeme.branchhub.application.security.model.AdminAuditLogResult;
import dev.jotaeme.branchhub.application.security.port.AdminAuditLogPort;

import java.util.Objects;

public class AdminListAuditLogUseCase {
    private final AdminAuditLogPort auditLogPort;

    public AdminListAuditLogUseCase(AdminAuditLogPort auditLogPort) {
        this.auditLogPort = Objects.requireNonNull(auditLogPort);
    }

    public PageResult<AdminAuditLogResult> execute(PageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query is required");
        }
        return auditLogPort.list(query);
    }
}
