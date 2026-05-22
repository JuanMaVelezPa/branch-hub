package com.jm.spring_web.entrypoints.rest;

import com.jm.spring_web.application.security.model.RefreshSessionListItem;
import com.jm.spring_web.application.security.usecase.ListRefreshSessionsUseCase;
import com.jm.spring_web.application.security.usecase.RevokeOtherRefreshSessionsUseCase;
import com.jm.spring_web.application.security.usecase.RevokeRefreshSessionUseCase;
import com.jm.spring_web.entrypoints.rest.dto.RefreshSessionResponse;
import com.jm.spring_web.entrypoints.rest.dto.RevokeOthersResponse;
import com.jm.spring_web.infrastructure.security.JwtProperties;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "Authentication — sessions")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/auth/sessions")
public class AuthSessionsController {

    private final ListRefreshSessionsUseCase listRefreshSessionsUseCase;
    private final RevokeRefreshSessionUseCase revokeRefreshSessionUseCase;
    private final RevokeOtherRefreshSessionsUseCase revokeOtherRefreshSessionsUseCase;
    private final JwtProperties jwtProperties;

    public AuthSessionsController(
            ListRefreshSessionsUseCase listRefreshSessionsUseCase,
            RevokeRefreshSessionUseCase revokeRefreshSessionUseCase,
            RevokeOtherRefreshSessionsUseCase revokeOtherRefreshSessionsUseCase,
            JwtProperties jwtProperties) {
        this.listRefreshSessionsUseCase = listRefreshSessionsUseCase;
        this.revokeRefreshSessionUseCase = revokeRefreshSessionUseCase;
        this.revokeOtherRefreshSessionsUseCase = revokeOtherRefreshSessionsUseCase;
        this.jwtProperties = jwtProperties;
    }

    @GetMapping
    public List<RefreshSessionResponse> listSessions(Authentication authentication, HttpServletRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        Optional<String> cookie = Optional.ofNullable(extractRefreshCookie(request));
        return listRefreshSessionsUseCase.execute(userId, cookie).stream()
                .map(AuthSessionsController::toResponse)
                .toList();
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> revokeSession(Authentication authentication, @PathVariable UUID sessionId) {
        UUID userId = UUID.fromString(authentication.getName());
        revokeRefreshSessionUseCase.execute(userId, sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/revoke-others")
    public RevokeOthersResponse revokeOtherSessions(Authentication authentication, HttpServletRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        String raw = extractRefreshCookie(request);
        int revoked = revokeOtherRefreshSessionsUseCase.execute(userId, raw);
        return new RevokeOthersResponse(revoked);
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (jwtProperties.refreshCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static RefreshSessionResponse toResponse(RefreshSessionListItem item) {
        return new RefreshSessionResponse(
                item.id(), item.familyId(), item.createdAt(), item.expiresAt(), item.current());
    }
}
