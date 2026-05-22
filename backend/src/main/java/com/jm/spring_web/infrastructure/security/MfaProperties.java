package com.jm.spring_web.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * MFA (TOTP) configuration.
 *
 * @param encryptionKeyBase64 AES-256 key (32 raw bytes) encoded as Base64.
 * @param issuer              Label for authenticator apps (issuer in otpauth URI).
 */
@ConfigurationProperties(prefix = "security.mfa")
public record MfaProperties(String encryptionKeyBase64, @DefaultValue("spring-web") String issuer) {
}
