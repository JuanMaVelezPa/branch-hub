package dev.jotaeme.branchhub.application.security.port;

import java.util.List;

public interface TokenProviderPort {
    String issueAccessToken(String subject, List<String> roles);

    String issueRefreshToken(String subject, List<String> roles);

    String extractSubjectFromAccessToken(String token);

    String extractSubjectFromRefreshToken(String token);
}
