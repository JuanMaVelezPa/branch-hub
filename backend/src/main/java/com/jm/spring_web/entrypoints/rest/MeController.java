package com.jm.spring_web.entrypoints.rest;

import com.jm.spring_web.application.security.model.AdminUserResult;
import com.jm.spring_web.application.security.usecase.AdminGetUserUseCase;
import com.jm.spring_web.entrypoints.rest.dto.MeResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Profile")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1")
public class MeController {
    private final AdminGetUserUseCase getUserUseCase;

    public MeController(AdminGetUserUseCase getUserUseCase) {
        this.getUserUseCase = getUserUseCase;
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        AdminUserResult user = getUserUseCase.execute(userId);
        return new MeResponse(
                user.id(),
                user.email(),
                user.enabled(),
                user.lockedUntil(),
                user.createdAt(),
                user.roles(),
                user.mfaEnabled());
    }
}
