package dev.jotaeme.branchhub.application.branch.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record BranchResult(
        UUID id,
        String code,
        String name,
        String city,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
