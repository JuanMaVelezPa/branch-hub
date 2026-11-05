package dev.jotaeme.branchhub.infrastructure.security;

import dev.jotaeme.branchhub.application.common.pagination.PageQuery;
import dev.jotaeme.branchhub.application.common.pagination.PageResult;
import dev.jotaeme.branchhub.application.security.model.AdminAuditLogResult;
import dev.jotaeme.branchhub.application.security.port.AdminAuditLogPort;
import dev.jotaeme.branchhub.infrastructure.persistence.SpringPageRequests;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamAuditLogJpaEntity;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamAuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class JpaAdminAuditLogAdapter implements AdminAuditLogPort {
    private final IamAuditLogRepository auditLogs;

    public JpaAdminAuditLogAdapter(IamAuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    @Override
    public PageResult<AdminAuditLogResult> list(PageQuery query) {
        Page<IamAuditLogJpaEntity> page = auditLogs.findAll(SpringPageRequests.from(query));
        return PageResult.of(
                page.getContent().stream().map(this::toResult).toList(),
                page.getTotalElements(),
                query.page(),
                query.size());
    }

    private AdminAuditLogResult toResult(IamAuditLogJpaEntity e) {
        return new AdminAuditLogResult(
                e.getId(),
                e.getActorUserId(),
                e.getAction(),
                e.getTargetUserId(),
                e.getMetadata(),
                e.getCreatedAt());
    }
}
