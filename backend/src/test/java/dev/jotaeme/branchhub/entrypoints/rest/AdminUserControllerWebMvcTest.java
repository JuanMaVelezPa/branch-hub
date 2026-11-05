package dev.jotaeme.branchhub.entrypoints.rest;

import dev.jotaeme.branchhub.support.AbstractPostgresIntegrationTest;
import dev.jotaeme.branchhub.application.common.pagination.PageQuery;
import dev.jotaeme.branchhub.application.common.pagination.PageResult;
import dev.jotaeme.branchhub.application.common.exception.UnprocessableEntityException;
import dev.jotaeme.branchhub.application.security.model.AdminUserResult;
import dev.jotaeme.branchhub.application.security.usecase.AdminCreateUserUseCase;
import dev.jotaeme.branchhub.application.security.usecase.AdminGetUserUseCase;
import dev.jotaeme.branchhub.application.security.usecase.AdminListUsersUseCase;
import dev.jotaeme.branchhub.application.security.usecase.AdminSetUserEnabledUseCase;
import dev.jotaeme.branchhub.application.security.usecase.AdminSetUserRolesUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AdminUserControllerWebMvcTest extends AbstractPostgresIntegrationTest {
    private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminCreateUserUseCase createUserUseCase;
    @MockitoBean
    private AdminGetUserUseCase getUserUseCase;
    @MockitoBean
    private AdminListUsersUseCase listUsersUseCase;
    @MockitoBean
    private AdminSetUserEnabledUseCase setUserEnabledUseCase;
    @MockitoBean
    private AdminSetUserRolesUseCase setUserRolesUseCase;

    @Test
    void shouldRejectUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000010", roles = "APP_ADMIN")
    void shouldRejectForbiddenForNonSuperAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "SUPER_ADMIN")
    void shouldListUsers() throws Exception {
        Mockito.when(listUsersUseCase.execute(ArgumentMatchers.any(PageQuery.class)))
                .thenReturn(PageResult.of(
                        List.of(new AdminUserResult(USER_ID, "u@example.com", true, null, Instant.now(), List.of("USER"))),
                        1,
                        0,
                        20
                ));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("u@example.com"))
                .andExpect(jsonPath("$.content[0].roles[0]").value("USER"));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "SUPER_ADMIN")
    void shouldCreateUser() throws Exception {
        Mockito.when(createUserUseCase.execute(Mockito.eq(ACTOR_ID), Mockito.any()))
                .thenReturn(new AdminUserResult(USER_ID, "new@example.com", true, null, Instant.now(), List.of("USER")));

        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@example.com","password":"Passw0rd!_2026","roles":["USER"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "SUPER_ADMIN")
    void shouldGetUserById() throws Exception {
        Mockito.when(getUserUseCase.execute(USER_ID))
                .thenReturn(new AdminUserResult(USER_ID, "u@example.com", true, null, Instant.now(), List.of("APP_ADMIN")));

        mockMvc.perform(get("/api/v1/admin/users/" + USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.roles[0]").value("APP_ADMIN"));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "SUPER_ADMIN")
    void shouldSetEnabled() throws Exception {
        Mockito.when(setUserEnabledUseCase.execute(Mockito.eq(ACTOR_ID), Mockito.eq(USER_ID), Mockito.eq(false)))
                .thenReturn(new AdminUserResult(USER_ID, "u@example.com", false, null, Instant.now(), List.of("USER")));

        mockMvc.perform(patch("/api/v1/admin/users/" + USER_ID + "/enabled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "SUPER_ADMIN")
    void shouldRejectDisablingOwnAccount() throws Exception {
        Mockito.when(setUserEnabledUseCase.execute(Mockito.eq(ACTOR_ID), Mockito.eq(ACTOR_ID), Mockito.eq(false)))
                .thenThrow(new UnprocessableEntityException("You cannot disable your own account"));

        mockMvc.perform(patch("/api/v1/admin/users/" + ACTOR_ID + "/enabled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enabled":false}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "SUPER_ADMIN")
    void shouldSetRoles() throws Exception {
        Mockito.when(setUserRolesUseCase.execute(Mockito.eq(ACTOR_ID), Mockito.eq(USER_ID), Mockito.eq(List.of("APP_ADMIN"))))
                .thenReturn(new AdminUserResult(USER_ID, "u@example.com", true, null, Instant.now(), List.of("APP_ADMIN")));

        mockMvc.perform(patch("/api/v1/admin/users/" + USER_ID + "/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roles":["APP_ADMIN"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("APP_ADMIN"));
    }
}

