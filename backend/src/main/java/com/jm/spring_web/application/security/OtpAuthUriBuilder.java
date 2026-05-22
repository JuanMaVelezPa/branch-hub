package com.jm.spring_web.application.security;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class OtpAuthUriBuilder {

    private OtpAuthUriBuilder() {
    }

    public static String totp(String issuer, String accountLabel, String secretBase32) {
        Objects.requireNonNull(issuer);
        Objects.requireNonNull(accountLabel);
        Objects.requireNonNull(secretBase32);
        String encLabel = URLEncoder.encode(issuer + ":" + accountLabel, StandardCharsets.UTF_8);
        String encIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        return "otpauth://totp/%s?secret=%s&issuer=%s&period=30&digits=6&algorithm=SHA1"
                .formatted(encLabel, secretBase32, encIssuer);
    }
}
