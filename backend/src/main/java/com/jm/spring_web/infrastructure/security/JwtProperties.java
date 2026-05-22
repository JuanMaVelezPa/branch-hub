package com.jm.spring_web.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String secret,
        long expirationSeconds,
        long refreshExpirationSeconds,
        String refreshCookieName,
        @DefaultValue("300") long mfaChallengeExpirationSeconds) {
}
