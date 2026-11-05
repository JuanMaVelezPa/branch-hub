package dev.jotaeme.branchhub.application.security.port;

import dev.jotaeme.branchhub.application.common.pagination.PageQuery;
import dev.jotaeme.branchhub.application.common.pagination.PageResult;
import dev.jotaeme.branchhub.application.security.model.AdminUserResult;
import dev.jotaeme.branchhub.application.security.model.CreateUserCommand;

import java.util.Optional;
import java.util.UUID;

public interface AdminUserPort {
    AdminUserResult create(CreateUserCommand command);

    Optional<AdminUserResult> findById(UUID id);

    PageResult<AdminUserResult> list(PageQuery query);

    AdminUserResult setEnabled(UUID userId, boolean enabled);

    AdminUserResult setRoles(UUID userId, java.util.List<String> roles);
}

