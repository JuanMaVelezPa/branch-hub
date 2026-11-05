package dev.jotaeme.branchhub.entrypoints.rest;

import dev.jotaeme.branchhub.support.AbstractPostgresIntegrationTest;
import dev.jotaeme.branchhub.application.common.pagination.PageQuery;
import dev.jotaeme.branchhub.application.common.pagination.PageResult;
import dev.jotaeme.branchhub.application.security.model.AdminAuditLogResult;
import dev.jotaeme.branchhub.application.security.usecase.AdminListAuditLogUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AdminAuditLogControllerWebMvcTest extends AbstractPostgresIntegrationTest {

    private static final UUID ENTRY_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c3");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminListAuditLogUseCase listAuditLogUseCase;

    @Test
    void shouldRejectUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000010", roles = "APP_ADMIN")
    void shouldRejectForbiddenForNonSuperAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "SUPER_ADMIN")
    void shouldListAuditLogs() throws Exception {
        Mockito.when(listAuditLogUseCase.execute(ArgumentMatchers.any(PageQuery.class)))
                .thenReturn(PageResult.of(
                        List.of(new AdminAuditLogResult(
                                ENTRY_ID,
                                ACTOR_ID,
                                "iam.user.create",
                                TARGET_ID,
                                "email=u@example.com",
                                Instant.parse("2026-04-16T12:00:00Z"))),
                        1,
                        0,
                        10));

        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("iam.user.create"))
                .andExpect(jsonPath("$.content[0].actorUserId").value(ACTOR_ID.toString()))
                .andExpect(jsonPath("$.content[0].targetUserId").value(TARGET_ID.toString()));
    }
}
