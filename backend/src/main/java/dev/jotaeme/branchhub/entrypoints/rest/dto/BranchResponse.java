package dev.jotaeme.branchhub.entrypoints.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record BranchResponse(
        UUID id,
        String code,
        String name,
        String city,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
