package dev.jotaeme.branchhub.application.security.model;

import java.util.List;
import java.util.UUID;

public record UserAccount(UUID id, List<String> roles) {
}

