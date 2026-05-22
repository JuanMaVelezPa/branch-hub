package com.jm.spring_web.application.security;

import com.jm.spring_web.application.security.model.UserAccount;

import java.util.List;

public final class AdminMfaPolicy {

    private AdminMfaPolicy() {
    }

    public static boolean isPrivilegedAdmin(List<String> roles) {
        if (roles == null) {
            return false;
        }
        return roles.stream().anyMatch(r -> {
            if (r == null) {
                return false;
            }
            String n = r.toUpperCase();
            return "SUPER_ADMIN".equals(n) || "APP_ADMIN".equals(n);
        });
    }

    public static boolean requiresSecondFactor(UserAccount account) {
        return account != null && account.mfaEnabled() && isPrivilegedAdmin(account.roles());
    }
}
