package com.jm.spring_web.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jm.spring_web.infrastructure.security.JwtProperties;
import com.jm.spring_web.infrastructure.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class BranchFlowIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("companydb")
            .withUsername("company")
            .withPassword("company");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("security.jwt.secret", () -> "test-secret-test-secret-test-secret-123");
        registry.add("security.jwt.expiration-seconds", () -> "3600");
        // Keep this test focused on the branch flow (not IAM bootstrap).
        registry.add("app.security.bootstrap.enabled", () -> "false");
    }

    @BeforeEach
    void cleanTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS branch (
                    id UUID PRIMARY KEY,
                    code VARCHAR(30) NOT NULL UNIQUE,
                    name VARCHAR(150) NOT NULL,
                    city VARCHAR(120) NOT NULL,
                    is_active BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS outbox_event (
                    id UUID PRIMARY KEY,
                    aggregate_type VARCHAR(80) NOT NULL,
                    aggregate_id UUID NOT NULL,
                    event_type VARCHAR(120) NOT NULL,
                    payload TEXT NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    retry_count INTEGER NOT NULL DEFAULT 0,
                    created_at TIMESTAMP NOT NULL,
                    processed_at TIMESTAMP NULL
                )
                """);
        jdbcTemplate.execute("DELETE FROM branch");
        jdbcTemplate.execute("DELETE FROM outbox_event");
    }

    @Test
    void shouldListBranchesWithPaginationRespectingPageSize() throws Exception {
        String token = loginAndGetToken();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 12; i++) {
            jdbcTemplate.update(
                    """
                            INSERT INTO branch (id, code, name, city, is_active, created_at, updated_at)
                            VALUES (?, ?, ?, ?, true, ?, ?)
                            """,
                    UUID.randomUUID(),
                    "BR-PAGE-" + i,
                    "Branch " + i,
                    "City",
                    now,
                    now);
        }

        mockMvc.perform(get("/api/v1/branches")
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/v1/branches")
                        .param("page", "1")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void shouldExecuteAuthenticatedBranchFlow() throws Exception {
        String token = loginAndGetToken();

        MvcResult createResult = mockMvc.perform(post("/api/v1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content("""
                                {
                                  "code":"BR100",
                                  "name":"HQ",
                                  "city":"Madrid"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("BR100"))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID branchId = UUID.fromString(created.get("id").asText());

        mockMvc.perform(get("/api/v1/branches/" + branchId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(branchId.toString()))
                .andExpect(jsonPath("$.isActive").value(true));

        mockMvc.perform(patch("/api/v1/branches/" + branchId + "/deactivate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    private String loginAndGetToken() throws Exception {
        JwtTokenService jwt = new JwtTokenService(new JwtProperties(
                "test-secret-test-secret-test-secret-123",
                3600,
                7200,
                "REFRESH_TOKEN",
                300));
        return jwt.issueAccessToken(UUID.fromString("00000000-0000-0000-0000-000000000010").toString(), List.of("APP_ADMIN"));
    }
}
