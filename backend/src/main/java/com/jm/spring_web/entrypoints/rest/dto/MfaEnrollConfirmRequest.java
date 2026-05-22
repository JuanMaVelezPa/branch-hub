package com.jm.spring_web.entrypoints.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "MfaEnrollConfirmRequest", description = "Confirm TOTP enrollment with a valid code from the pending secret")
public record MfaEnrollConfirmRequest(
        @NotBlank @Schema(description = "6-digit TOTP from the app", example = "123456") String code) {
}
