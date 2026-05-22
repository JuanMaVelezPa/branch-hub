package com.jm.spring_web.application.security.model;

import java.util.UUID;

public record RefreshRotationOutcome(UUID userId, String newRawRefreshToken) {
}
