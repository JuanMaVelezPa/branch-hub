package dev.jotaeme.branchhub.application.security.model;

public record AuthResult(String token, String refreshToken) {
}
