package com.jm.spring_web.entrypoints.rest;

import com.jm.spring_web.application.security.model.RefreshSessionListItem;
import com.jm.spring_web.application.security.usecase.ListRefreshSessionsUseCase;
import com.jm.spring_web.application.security.usecase.RevokeOtherRefreshSessionsUseCase;
import com.jm.spring_web.application.security.usecase.RevokeRefreshSessionUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSessionsControllerWebMvcTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListRefreshSessionsUseCase listRefreshSessionsUseCase;

    @MockitoBean
    private RevokeRefreshSessionUseCase revokeRefreshSessionUseCase;

    @MockitoBean
    private RevokeOtherRefreshSessionsUseCase revokeOtherRefreshSessionsUseCase;

    @Test
    void shouldRejectListWhenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/sessions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000099", roles = "USER")
    void shouldListSessions() throws Exception {
        Mockito.when(listRefreshSessionsUseCase.execute(Mockito.eq(USER_ID), Mockito.any()))
                .thenReturn(List.of(new RefreshSessionListItem(
                        SESSION_ID,
                        UUID.fromString("00000000-0000-0000-0000-0000000000bb"),
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-02-01T00:00:00Z"),
                        true)));

        mockMvc.perform(get("/api/v1/auth/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(SESSION_ID.toString()))
                .andExpect(jsonPath("$[0].current").value(true));
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000099", roles = "USER")
    void shouldRevokeSession() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/sessions/{id}", SESSION_ID))
                .andExpect(status().isNoContent());
        Mockito.verify(revokeRefreshSessionUseCase).execute(USER_ID, SESSION_ID);
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000099", roles = "USER")
    void shouldRevokeOthers() throws Exception {
        Mockito.when(revokeOtherRefreshSessionsUseCase.execute(Mockito.eq(USER_ID), Mockito.any()))
                .thenReturn(2);

        mockMvc.perform(post("/api/v1/auth/sessions/revoke-others").cookie(new jakarta.servlet.http.Cookie("REFRESH_TOKEN", "tok")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revokedCount").value(2));
    }
}
