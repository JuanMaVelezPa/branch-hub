package dev.jotaeme.branchhub.entrypoints.rest;

import dev.jotaeme.branchhub.application.common.pagination.PageQuery;
import dev.jotaeme.branchhub.application.branch.model.BranchResult;
import dev.jotaeme.branchhub.application.common.pagination.PageResult;
import dev.jotaeme.branchhub.application.branch.usecase.CreateBranchUseCase;
import dev.jotaeme.branchhub.application.branch.usecase.DeactivateBranchUseCase;
import dev.jotaeme.branchhub.application.branch.usecase.GetBranchUseCase;
import dev.jotaeme.branchhub.application.branch.usecase.ListBranchesUseCase;
import dev.jotaeme.branchhub.application.branch.usecase.UpdateBranchUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BranchControllerWebMvcTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateBranchUseCase createBranchUseCase;
    @MockitoBean
    private GetBranchUseCase getBranchUseCase;
    @MockitoBean
    private ListBranchesUseCase listBranchesUseCase;
    @MockitoBean
    private UpdateBranchUseCase updateBranchUseCase;
    @MockitoBean
    private DeactivateBranchUseCase deactivateBranchUseCase;
    @Test
    void shouldRejectUnauthorizedRequestForProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/branches"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").value("Authentication is required to access this resource"));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void shouldReturnForbiddenWhenRoleIsInsufficient() throws Exception {
        mockMvc.perform(get("/api/v1/branches"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "APP_ADMIN")
    void shouldReturnPagedListWhenAuthenticated() throws Exception {
        Mockito.when(listBranchesUseCase.execute(ArgumentMatchers.any(PageQuery.class))).thenReturn(new PageResult<>(
                List.of(new BranchResult(
                        UUID.randomUUID(),
                        "BR001",
                        "Main",
                        "Madrid",
                        true,
                        LocalDateTime.now(),
                        LocalDateTime.now())),
                1,
                0,
                10,
                1));

        mockMvc.perform(get("/api/v1/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("BR001"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @WithMockUser(username = "admin", roles = "APP_ADMIN")
    void shouldReturnBadRequestWhenPageSizeExceedsMax() throws Exception {
        mockMvc.perform(get("/api/v1/branches").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "APP_ADMIN")
    void shouldReturnBadRequestWhenSortFieldInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/branches").param("sort", "unknown,asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "APP_ADMIN")
    void shouldReturnBadRequestWhenCreatePayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"",
                                  "name":"",
                                  "city":""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").exists())
                .andExpect(jsonPath("$.errors[0].message").exists());
    }
}
