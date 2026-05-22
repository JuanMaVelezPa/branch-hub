package com.jm.spring_web.application.security.port;

import com.jm.spring_web.application.common.pagination.PageQuery;
import com.jm.spring_web.application.common.pagination.PageResult;
import com.jm.spring_web.application.security.model.AdminSetEnabledOutcome;
import com.jm.spring_web.application.security.model.AdminUserResult;
import com.jm.spring_web.application.security.model.CreateUserCommand;

import java.util.Optional;
import java.util.UUID;

public interface AdminUserPort {
    AdminUserResult create(CreateUserCommand command);

    Optional<AdminUserResult> findById(UUID id);

    PageResult<AdminUserResult> list(PageQuery query);

    AdminSetEnabledOutcome setEnabled(UUID userId, boolean enabled);

    AdminUserResult setRoles(UUID userId, java.util.List<String> roles);
}

