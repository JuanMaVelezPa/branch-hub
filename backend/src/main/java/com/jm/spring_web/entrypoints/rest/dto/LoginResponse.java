package com.jm.spring_web.entrypoints.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
        name = "LoginResponse",
        description = "Login or refresh result. Either `token` (access JWT; refresh cookie may be set) or `mfaChallengeToken` when a privileged admin must complete TOTP.")
public record LoginResponse(
        @Schema(description = "Access JWT") String token,
        @Schema(description = "Short-lived JWT; send to POST /api/v1/auth/mfa/verify with the 6-digit TOTP code") String mfaChallengeToken) {
}
