package dev.jotaeme.branchhub.application.security.model;

public record AuthenticateCommand(String username, String password) {
}
