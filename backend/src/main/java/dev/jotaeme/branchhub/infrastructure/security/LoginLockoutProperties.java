package dev.jotaeme.branchhub.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.login")
public class LoginLockoutProperties {
    /**
     * Failed password attempts before {@code locked_until} is set.
     */
    private int maxFailedAttempts = 5;

    /**
     * How long the account stays locked after exceeding max failures.
     */
    private int lockoutMinutes = 15;

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public void setMaxFailedAttempts(int maxFailedAttempts) {
        this.maxFailedAttempts = maxFailedAttempts;
    }

    public int getLockoutMinutes() {
        return lockoutMinutes;
    }

    public void setLockoutMinutes(int lockoutMinutes) {
        this.lockoutMinutes = lockoutMinutes;
    }
}
