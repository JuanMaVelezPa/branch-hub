package dev.jotaeme.branchhub.application.security;

import dev.jotaeme.branchhub.application.common.pagination.SortDirection;
import dev.jotaeme.branchhub.application.common.pagination.SortOrder;
import dev.jotaeme.branchhub.application.common.pagination.SortPolicy;

import java.util.Set;

public final class IamAuditLogListPagination {
    private IamAuditLogListPagination() {
    }

    public static final SortPolicy SORT_POLICY = new SortPolicy(
            new SortOrder("createdAt", SortDirection.DESC),
            Set.of("createdAt", "action"));
}
