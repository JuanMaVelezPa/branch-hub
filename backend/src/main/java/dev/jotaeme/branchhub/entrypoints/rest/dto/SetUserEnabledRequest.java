package dev.jotaeme.branchhub.entrypoints.rest.dto;

import jakarta.validation.constraints.NotNull;

public record SetUserEnabledRequest(@NotNull Boolean enabled) {
}

