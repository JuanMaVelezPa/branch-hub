package dev.jotaeme.branchhub.application.security.usecase;

import dev.jotaeme.branchhub.application.common.exception.NotFoundException;
import dev.jotaeme.branchhub.application.security.model.AdminUserResult;
import dev.jotaeme.branchhub.application.security.port.AdminUserPort;

import java.util.Objects;
import java.util.UUID;

public class AdminGetUserUseCase {
    private final AdminUserPort adminUserPort;

    public AdminGetUserUseCase(AdminUserPort adminUserPort) {
        this.adminUserPort = Objects.requireNonNull(adminUserPort);
    }

    public AdminUserResult execute(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        return adminUserPort.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}

