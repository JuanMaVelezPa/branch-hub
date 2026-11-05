package dev.jotaeme.branchhub.application.security;

import java.util.regex.Pattern;

public final class EmailPolicy {
    private static final Pattern SIMPLE_EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private EmailPolicy() {
    }

    public static boolean isValid(String email) {
        if (email == null) {
            return false;
        }
        return SIMPLE_EMAIL_PATTERN.matcher(email.trim()).matches();
    }
}

