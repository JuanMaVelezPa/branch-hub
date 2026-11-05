package dev.jotaeme.branchhub.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String secret,
        long expirationSeconds,
        long refreshExpirationSeconds,
        String refreshCookieName) {
}
