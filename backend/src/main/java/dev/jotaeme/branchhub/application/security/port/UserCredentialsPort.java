package dev.jotaeme.branchhub.application.security.port;

public interface UserCredentialsPort {
    boolean authenticate(String username, String rawPassword);
}
