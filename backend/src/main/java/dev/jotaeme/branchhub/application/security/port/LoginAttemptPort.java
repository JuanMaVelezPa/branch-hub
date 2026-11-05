package dev.jotaeme.branchhub.application.security.port;

/**
 * Tracks failed/successful password logins for account lockout (IAM1).
 */
public interface LoginAttemptPort {
    void onFailedLogin(String attemptedEmail);

    void onSuccessfulLogin(String email);
}
