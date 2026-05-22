package com.jm.spring_web.entrypoints.rest;

import com.jm.spring_web.application.common.pagination.PageQuery;
import com.jm.spring_web.application.security.IamAuditLogListPagination;
import com.jm.spring_web.application.security.usecase.AdminListAuditLogUseCase;
import com.jm.spring_web.entrypoints.rest.dto.AdminAuditLogResponse;
import com.jm.spring_web.entrypoints.rest.dto.PageRequestParams;
import com.jm.spring_web.entrypoints.rest.dto.PagedResponse;
import com.jm.spring_web.entrypoints.rest.mapper.PageMapper;
import com.jm.spring_web.entrypoints.rest.pagination.PaginationBinding;
import com.jm.spring_web.infrastructure.observability.AppMetrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin — audit log")
@SecurityRequirement(name = "bearerAuth")
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

    private AdminAuditLogResponse toResponse(com.jm.spring_web.application.security.model.AdminAuditLogResult r) {
        return new AdminAuditLogResponse(
                r.id(), r.actorUserId(), r.action(), r.targetUserId(), r.metadata(), r.createdAt());
    }
}
