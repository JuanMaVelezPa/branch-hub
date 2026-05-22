package com.jm.spring_web.entrypoints.rest;

import com.jm.spring_web.entrypoints.rest.dto.AdminRoleResponse;
import com.jm.spring_web.infrastructure.observability.AppMetrics;
import com.jm.spring_web.infrastructure.persistence.iam.IamRoleRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@Tag(name = "Admin — roles")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/roles")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminRoleController {
    private final IamRoleRepository roles;
    private final AppMetrics appMetrics;

    public AdminRoleController(IamRoleRepository roles, AppMetrics appMetrics) {
        this.roles = roles;
        this.appMetrics = appMetrics;
    }

    @GetMapping
    public List<AdminRoleResponse> list() {
        try {
            List<AdminRoleResponse> result = roles.findAll().stream()
                    .sorted(Comparator.comparing(r -> r.getName() == null ? "" : r.getName()))
                    .map(r -> new AdminRoleResponse(r.getId(), r.getName()))
                    .toList();
            appMetrics.incrementIamAdminAction("role.list", "success");
            return result;
        } catch (RuntimeException ex) {
            appMetrics.incrementIamAdminAction("role.list", "failure");
            throw ex;
        }
    }
}

