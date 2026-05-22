package com.jm.spring_web.integration;

import com.jm.spring_web.application.security.model.AuthResult;
import com.jm.spring_web.application.security.usecase.AuthenticateUserUseCase;
import com.jm.spring_web.application.security.usecase.RefreshTokenUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
        properties = {
            "security.auth-rate-limit.enabled=true",
            "security.auth-rate-limit.login-max-requests=500",
            "security.auth-rate-limit.login-window=PT1M",
            "security.auth-rate-limit.login-identifier-max-requests=2",
            "security.auth-rate-limit.login-identifier-window=PT1M",
            "security.auth-rate-limit.refresh-max-requests=500",
            "security.auth-rate-limit.refresh-window=PT1M",
            "security.auth-rate-limit.cache-max-entries=10000",
            "security.auth-rate-limit.cache-expire-after-access=PT10M",
        })
class AuthRateLimitLoginIdentifierIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticateUserUseCase authenticateUserUseCase;

    @MockitoBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @Test
    void shouldReturn429WhenSameLoginIdentifierExceededAcrossManyIps() throws Exception {
        Mockito.when(authenticateUserUseCase.execute(Mockito.any()))
                .thenReturn(new AuthResult("access", "refresh", null));

        String body = """
                {"username":"victim@example.com","password":"x"}
                """;
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", "192.168.0." + (10 + i))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "10.0.0.99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }
}
