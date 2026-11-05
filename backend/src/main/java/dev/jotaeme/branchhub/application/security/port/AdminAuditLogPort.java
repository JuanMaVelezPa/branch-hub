package dev.jotaeme.branchhub.application.security.port;

import dev.jotaeme.branchhub.application.common.pagination.PageQuery;
import dev.jotaeme.branchhub.application.common.pagination.PageResult;
import dev.jotaeme.branchhub.application.security.model.AdminAuditLogResult;

public interface AdminAuditLogPort {
    PageResult<AdminAuditLogResult> list(PageQuery query);
}
