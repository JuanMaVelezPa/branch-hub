package dev.jotaeme.branchhub.entrypoints.rest.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SetUserRolesRequest(@NotEmpty List<String> roles) {
}

