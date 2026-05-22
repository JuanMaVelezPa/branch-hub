package com.jm.spring_web.entrypoints.rest;

import com.jm.spring_web.application.security.model.AuthResult;
import com.jm.spring_web.application.security.model.VerifyMfaCommand;
import com.jm.spring_web.application.security.usecase.BeginAdminMfaEnrollmentUseCase;
import com.jm.spring_web.application.security.usecase.ConfirmAdminMfaEnrollmentUseCase;
import com.jm.spring_web.application.security.usecase.VerifyMfaChallengeUseCase;
import com.jm.spring_web.entrypoints.rest.dto.LoginResponse;
import com.jm.spring_web.entrypoints.rest.dto.MfaEnrollBeginResponse;
import com.jm.spring_web.entrypoints.rest.dto.MfaEnrollConfirmRequest;
import com.jm.spring_web.entrypoints.rest.dto.MfaVerifyRequest;
import com.jm.spring_web.infrastructure.observability.AppMetrics;
import com.jm.spring_web.infrastructure.security.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

@Tag(name = "Authentication — MFA")
@RestController
@RequestMapping("/api/v1/auth/mfa")
public class MfaAuthController {
    private final VerifyMfaChallengeUseCase verifyMfaChallengeUseCase;
    private final BeginAdminMfaEnrollmentUseCase beginAdminMfaEnrollmentUseCase;
    private final ConfirmAdminMfaEnrollmentUseCase confirmAdminMfaEnrollmentUseCase;
    private final JwtProperties jwtProperties;
    private final AppMetrics appMetrics;
    private final boolean secureCookies;

    public MfaAuthController(
            VerifyMfaChallengeUseCase verifyMfaChallengeUseCase,
            BeginAdminMfaEnrollmentUseCase beginAdminMfaEnrollmentUseCase,
            ConfirmAdminMfaEnrollmentUseCase confirmAdminMfaEnrollmentUseCase,
            JwtProperties jwtProperties,
            AppMetrics appMetrics,
            @Value("${security.cookies.secure:false}") boolean secureCookies) {
        this.verifyMfaChallengeUseCase = verifyMfaChallengeUseCase;
        this.beginAdminMfaEnrollmentUseCase = beginAdminMfaEnrollmentUseCase;
        this.confirmAdminMfaEnrollmentUseCase = confirmAdminMfaEnrollmentUseCase;
        this.jwtProperties = jwtProperties;
        this.appMetrics = appMetrics;
        this.secureCookies = secureCookies;
    }

    @Operation(summary = "Complete MFA login", description = "Public. Validates TOTP and returns access token + refresh cookie.")
    @PostMapping("/verify")
    public LoginResponse verify(@Valid @RequestBody MfaVerifyRequest request, HttpServletResponse response) {
        AuthResult result = verifyMfaChallengeUseCase.execute(
                new VerifyMfaCommand(request.mfaChallengeToken(), request.code()));
        addRefreshCookie(response, result.refreshToken());
        appMetrics.incrementAuthLoginSuccess();
        return new LoginResponse(result.token(), null);
    }

    @Operation(summary = "Begin TOTP enrollment")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/enroll/begin")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APP_ADMIN')")
    public MfaEnrollBeginResponse enrollBegin(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        var r = beginAdminMfaEnrollmentUseCase.execute(userId);
        return new MfaEnrollBeginResponse(r.otpauthUri(), r.secretBase32());
    }

    @Operation(summary = "Confirm TOTP enrollment")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/enroll/confirm")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','APP_ADMIN')")
    public void enrollConfirm(Authentication authentication, @Valid @RequestBody MfaEnrollConfirmRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        confirmAdminMfaEnrollmentUseCase.execute(userId, request.code());
    }

    private void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(jwtProperties.refreshCookieName(), refreshToken)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(jwtProperties.refreshExpirationSeconds())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
