package dev.jotaeme.branchhub.entrypoints.rest;

import dev.jotaeme.branchhub.support.AbstractPostgresIntegrationTest;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamRoleJpaEntity;
import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamRoleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "app.security.bootstrap.enabled=false")
@AutoConfigureMockMvc
class AdminRoleControllerWebMvcTest extends AbstractPostgresIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IamRoleRepository roleRepository;

    @Test
    void shouldRejectUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000010", roles = "APP_ADMIN")
    void shouldRejectForbiddenForNonSuperAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "SUPER_ADMIN")
    void shouldListRolesSorted() throws Exception {
        Mockito.when(roleRepository.findAll()).thenReturn(List.of(
                new IamRoleJpaEntity(UUID.randomUUID(), "USER", Instant.now()),
                new IamRoleJpaEntity(UUID.randomUUID(), "APP_ADMIN", Instant.now()),
                new IamRoleJpaEntity(UUID.randomUUID(), "SUPER_ADMIN", Instant.now())
        ));

        mockMvc.perform(get("/api/v1/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("APP_ADMIN"))
                .andExpect(jsonPath("$[1].name").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$[2].name").value("USER"));
    }
}

