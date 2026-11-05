package dev.jotaeme.branchhub.entrypoints.rest;

import dev.jotaeme.branchhub.application.common.pagination.PageQuery;
import dev.jotaeme.branchhub.application.security.IamAuditLogListPagination;
import dev.jotaeme.branchhub.application.security.usecase.AdminListAuditLogUseCase;
import dev.jotaeme.branchhub.entrypoints.rest.dto.AdminAuditLogResponse;
import dev.jotaeme.branchhub.entrypoints.rest.dto.PageRequestParams;
import dev.jotaeme.branchhub.entrypoints.rest.dto.PagedResponse;
import dev.jotaeme.branchhub.entrypoints.rest.mapper.PageMapper;
import dev.jotaeme.branchhub.entrypoints.rest.pagination.PaginationBinding;
import dev.jotaeme.branchhub.infrastructure.observability.AppMetrics;
import io.swagger.v3.oas.annotations.Operation;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminAuditLogController {
    private final AdminListAuditLogUseCase listAuditLogUseCase;
    private final AppMetrics appMetrics;

    public AdminAuditLogController(AdminListAuditLogUseCase listAuditLogUseCase, AppMetrics appMetrics) {
        this.listAuditLogUseCase = listAuditLogUseCase;
        this.appMetrics = appMetrics;
    }

    @GetMapping
    @Operation(operationId = "listAuditLogs", summary = "List IAM audit log entries (newest first by default)")
    public PagedResponse<AdminAuditLogResponse> list(@ParameterObject PageRequestParams pagination) {
        try {
            PageQuery query = PaginationBinding.toPageQuery(pagination, IamAuditLogListPagination.SORT_POLICY);
            var result = PageMapper.toPagedResponse(listAuditLogUseCase.execute(query), this::toResponse);
            appMetrics.incrementIamAdminAction("audit.list", "success");
            return result;
        } catch (RuntimeException ex) {
            appMetrics.incrementIamAdminAction("audit.list", "failure");
            throw ex;
        }
    }

    private AdminAuditLogResponse toResponse(dev.jotaeme.branchhub.application.security.model.AdminAuditLogResult r) {
        return new AdminAuditLogResponse(
                r.id(), r.actorUserId(), r.action(), r.targetUserId(), r.metadata(), r.createdAt());
    }
}
