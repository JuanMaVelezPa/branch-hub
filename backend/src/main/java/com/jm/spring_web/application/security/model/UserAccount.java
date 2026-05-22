package com.jm.spring_web.application.security.model;

import java.util.List;
import java.util.UUID;

public record UserAccount(UUID id, String email, List<String> roles, boolean mfaEnabled) {
}

