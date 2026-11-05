package dev.jotaeme.branchhub.entrypoints.rest;

import dev.jotaeme.branchhub.application.common.pagination.PageQuery;
import dev.jotaeme.branchhub.application.security.IamUserListPagination;
import dev.jotaeme.branchhub.application.security.model.CreateUserCommand;
import dev.jotaeme.branchhub.application.security.usecase.AdminCreateUserUseCase;
import dev.jotaeme.branchhub.application.security.usecase.AdminGetUserUseCase;
import dev.jotaeme.branchhub.application.security.usecase.AdminListUsersUseCase;
import dev.jotaeme.branchhub.application.security.usecase.AdminSetUserEnabledUseCase;
import dev.jotaeme.branchhub.application.security.usecase.AdminSetUserRolesUseCase;
import dev.jotaeme.branchhub.entrypoints.rest.dto.AdminUserResponse;
import dev.jotaeme.branchhub.entrypoints.rest.dto.CreateUserRequest;
import dev.jotaeme.branchhub.entrypoints.rest.dto.PageRequestParams;
import dev.jotaeme.branchhub.entrypoints.rest.dto.PagedResponse;
import dev.jotaeme.branchhub.entrypoints.rest.dto.SetUserEnabledRequest;
import dev.jotaeme.branchhub.entrypoints.rest.dto.SetUserRolesRequest;
import dev.jotaeme.branchhub.entrypoints.rest.mapper.PageMapper;
import dev.jotaeme.branchhub.entrypoints.rest.pagination.PaginationBinding;
import dev.jotaeme.branchhub.infrastructure.observability.AppMetrics;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserController {
    private final AdminCreateUserUseCase createUserUseCase;
    private final AdminGetUserUseCase getUserUseCase;
    private final AdminListUsersUseCase listUsersUseCase;
    private final AdminSetUserEnabledUseCase setUserEnabledUseCase;
    private final AdminSetUserRolesUseCase setUserRolesUseCase;
    private final AppMetrics appMetrics;

    public AdminUserController(
            AdminCreateUserUseCase createUserUseCase,
            AdminGetUserUseCase getUserUseCase,
            AdminListUsersUseCase listUsersUseCase,
            AdminSetUserEnabledUseCase setUserEnabledUseCase,
            AdminSetUserRolesUseCase setUserRolesUseCase,
            AppMetrics appMetrics) {
        this.createUserUseCase = createUserUseCase;
        this.getUserUseCase = getUserUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.setUserEnabledUseCase = setUserEnabledUseCase;
        this.setUserRolesUseCase = setUserRolesUseCase;
        this.appMetrics = appMetrics;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserResponse create(Authentication authentication, @Valid @RequestBody CreateUserRequest request) {
        UUID actorId = actorId(authentication);
        try {
            var created = createUserUseCase.execute(
                    actorId,
                    new CreateUserCommand(request.email(), request.password(), request.roles()));
            appMetrics.incrementIamAdminAction("user.create", "success");
            return toResponse(created);
        } catch (RuntimeException ex) {
            appMetrics.incrementIamAdminAction("user.create", "failure");
            throw ex;
        }
    }

    @GetMapping("/{id}")
    public AdminUserResponse getById(@PathVariable UUID id) {
        try {
            var result = getUserUseCase.execute(id);
            appMetrics.incrementIamAdminAction("user.get", "success");
            return toResponse(result);
        } catch (RuntimeException ex) {
            appMetrics.incrementIamAdminAction("user.get", "failure");
            throw ex;
        }
    }

    @GetMapping
    public PagedResponse<AdminUserResponse> list(@ParameterObject PageRequestParams pagination) {
        try {
            PageQuery query = PaginationBinding.toPageQuery(pagination, IamUserListPagination.SORT_POLICY);
            var result = PageMapper.toPagedResponse(listUsersUseCase.execute(query), this::toResponse);
            appMetrics.incrementIamAdminAction("user.list", "success");
            return result;
        } catch (RuntimeException ex) {
            appMetrics.incrementIamAdminAction("user.list", "failure");
            throw ex;
        }
    }

    @PatchMapping("/{id}/enabled")
    public AdminUserResponse setEnabled(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody SetUserEnabledRequest request) {
        UUID actorId = actorId(authentication);
        try {
            var updated = setUserEnabledUseCase.execute(actorId, id, request.enabled());
            appMetrics.incrementIamAdminAction("user.set_enabled", "success");
            return toResponse(updated);
        } catch (RuntimeException ex) {
            appMetrics.incrementIamAdminAction("user.set_enabled", "failure");
            throw ex;
        }
    }

    @PatchMapping("/{id}/roles")
    public AdminUserResponse setRoles(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody SetUserRolesRequest request) {
        UUID actorId = actorId(authentication);
        try {
            var updated = setUserRolesUseCase.execute(actorId, id, request.roles());
            appMetrics.incrementIamAdminAction("user.set_roles", "success");
            return toResponse(updated);
        } catch (RuntimeException ex) {
            appMetrics.incrementIamAdminAction("user.set_roles", "failure");
            throw ex;
        }
    }

    private UUID actorId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Missing authentication");
        }
        return UUID.fromString(authentication.getName());
    }

    private AdminUserResponse toResponse(dev.jotaeme.branchhub.application.security.model.AdminUserResult r) {
        return new AdminUserResponse(
                r.id(),
                r.email(),
                r.enabled(),
                r.lockedUntil(),
                r.createdAt(),
                r.roles()
        );
    }
}

