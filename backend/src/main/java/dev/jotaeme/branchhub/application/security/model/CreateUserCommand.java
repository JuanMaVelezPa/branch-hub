package dev.jotaeme.branchhub.application.security.model;

import java.util.List;

public record CreateUserCommand(
        String email,
        String rawPassword,
        List<String> roles
) {
}

