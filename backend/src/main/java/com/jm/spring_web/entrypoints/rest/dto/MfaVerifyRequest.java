package com.jm.spring_web.entrypoints.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "MfaVerifyRequest", description = "Complete MFA login after password step")
public record MfaVerifyRequest(
        @NotBlank @Schema(description = "JWT from login response when `mfaChallengeToken` was returned") String mfaChallengeToken,
        @NotBlank @Schema(description = "6-digit TOTP code", example = "123456") String code) {
}
