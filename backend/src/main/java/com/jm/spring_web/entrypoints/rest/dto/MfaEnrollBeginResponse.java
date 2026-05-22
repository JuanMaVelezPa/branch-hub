package com.jm.spring_web.entrypoints.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MfaEnrollBeginResponse", description = "TOTP enrollment payload for authenticator apps")
public record MfaEnrollBeginResponse(
        @Schema(description = "otpauth:// URI (QR)") String otpauthUri,
        @Schema(description = "Base32 secret for manual entry") String secretBase32) {
}
