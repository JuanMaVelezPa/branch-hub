package dev.jotaeme.branchhub.application.security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class PasswordPolicy {
    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 72;

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern NUMBER = Pattern.compile("\\d");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z\\d]");

    private PasswordPolicy() {
    }

    public static List<String> violations(String rawPassword) {
        List<String> issues = new ArrayList<>();
        if (rawPassword == null || rawPassword.isBlank()) {
            issues.add("Password is required");
            return issues;
        }
        if (rawPassword.length() < MIN_LENGTH || rawPassword.length() > MAX_LENGTH) {
            issues.add("Password length must be between %d and %d characters".formatted(MIN_LENGTH, MAX_LENGTH));
        }
        if (!UPPER.matcher(rawPassword).find()) {
            issues.add("Password must include an uppercase letter");
        }
        if (!LOWER.matcher(rawPassword).find()) {
            issues.add("Password must include a lowercase letter");
        }
        if (!NUMBER.matcher(rawPassword).find()) {
            issues.add("Password must include a number");
        }
        if (!SPECIAL.matcher(rawPassword).find()) {
            issues.add("Password must include a special character");
        }
        return issues;
    }

    public static boolean isValid(String rawPassword) {
        return violations(rawPassword).isEmpty();
    }
}

