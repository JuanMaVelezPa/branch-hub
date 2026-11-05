package dev.jotaeme.branchhub.entrypoints.rest;

import dev.jotaeme.branchhub.application.security.model.AdminUserResult;
import dev.jotaeme.branchhub.application.security.usecase.AdminGetUserUseCase;
import dev.jotaeme.branchhub.entrypoints.rest.dto.MeResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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
                user.roles());
    }
}
