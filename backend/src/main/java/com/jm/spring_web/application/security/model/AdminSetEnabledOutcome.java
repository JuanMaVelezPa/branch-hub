package com.jm.spring_web.application.security.model;

import java.util.Objects;

/**
 * Result of toggling {@code iam_user.enabled}, including whether the account just became active.
 */
public record AdminSetEnabledOutcome(AdminUserResult user, boolean becameActive) {
    public AdminSetEnabledOutcome {
        Objects.requireNonNull(user);
    }
}
