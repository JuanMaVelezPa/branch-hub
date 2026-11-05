package dev.jotaeme.branchhub.application.security.usecase;

import dev.jotaeme.branchhub.application.common.pagination.PageQuery;
import dev.jotaeme.branchhub.application.common.pagination.PageResult;
import dev.jotaeme.branchhub.application.security.model.AdminUserResult;
import dev.jotaeme.branchhub.application.security.port.AdminUserPort;

import java.util.Objects;

public class AdminListUsersUseCase {
    private final AdminUserPort adminUserPort;

    public AdminListUsersUseCase(AdminUserPort adminUserPort) {
        this.adminUserPort = Objects.requireNonNull(adminUserPort);
    }

    public PageResult<AdminUserResult> execute(PageQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query is required");
        }
        return adminUserPort.list(query);
    }
}

