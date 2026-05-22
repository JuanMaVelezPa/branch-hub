package com.jm.spring_web.entrypoints.rest;

import com.jm.spring_web.application.security.model.AuthResult;
import com.jm.spring_web.application.security.port.RefreshSessionPort;
import com.jm.spring_web.application.security.usecase.AuthenticateUserUseCase;
import com.jm.spring_web.application.security.usecase.RefreshTokenUseCase;
import com.jm.spring_web.application.common.exception.UnauthorizedException;
import com.jm.spring_web.application.common.exception.UnprocessableEntityException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerWebMvcTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticateUserUseCase authenticateUserUseCase;
    @MockitoBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockitoBean
    private RefreshSessionPort refreshSessionPort;

    @Test
    void shouldLoginSuccessfullyWithValidPayload() throws Exception {
        Mockito.when(authenticateUserUseCase.execute(Mockito.any()))
                .thenReturn(new AuthResult("jwt-token", "refresh-token", null));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"admin",
                                  "password":"admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().exists(HttpHeaders.SET_COOKIE));
    }

    @Test
    void shouldReturnMfaChallengeWithoutRefreshCookieWhenSecondFactorRequired() throws Exception {
        Mockito.when(authenticateUserUseCase.execute(Mockito.any()))
                .thenReturn(new AuthResult(null, null, "mfa-jwt"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"admin",
                                  "password":"admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaChallengeToken").value("mfa-jwt"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void shouldReturnBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"",
                                  "password":""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").exists())
                .andExpect(jsonPath("$.errors[0].message").exists());
        verify(authenticateUserUseCase, never()).execute(Mockito.any());
    }

    @Test
    void shouldReturnUnprocessableWhenCredentialsAreInvalid() throws Exception {
        Mockito.when(authenticateUserUseCase.execute(Mockito.any()))
                .thenThrow(new UnprocessableEntityException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"admin",
                                  "password":"wrong"
                                }
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("Invalid credentials"));
    }

    @Test
    void shouldRefreshAccessTokenWithValidRefreshCookie() throws Exception {
        Mockito.when(refreshTokenUseCase.execute(Mockito.any()))
                .thenReturn(new AuthResult("new-access-token", "new-refresh-token", null));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("REFRESH_TOKEN", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-access-token"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().exists(HttpHeaders.SET_COOKIE));
    }

    @Test
    void shouldReturnUnauthorizedWhenRefreshCookieIsMissing() throws Exception {
        Mockito.when(refreshTokenUseCase.execute(Mockito.any()))
                .thenThrow(new UnauthorizedException("Refresh token is required"));

        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Refresh token is required"));
    }

    @Test
    void shouldLogoutWithNoContentAndClearRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("REFRESH_TOKEN")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
        verify(refreshTokenUseCase, never()).execute(Mockito.any());
        verify(refreshSessionPort).revokePresentedTokenIfActive(null);
    }

    @Test
    void shouldRevokeServerSessionOnLogoutWhenCookiePresent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("REFRESH_TOKEN", "rt-secret")))
                .andExpect(status().isNoContent());
        verify(refreshSessionPort).revokePresentedTokenIfActive("rt-secret");
    }
}
