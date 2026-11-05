package dev.jotaeme.branchhub.entrypoints.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBranchRequest(
        @NotBlank
        @Size(max = 30)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$")
        String code,
        @NotBlank
        @Size(max = 150)
        String name,
        @NotBlank
        @Size(max = 120)
        String city) {
}
