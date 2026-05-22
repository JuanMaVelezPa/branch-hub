package com.jm.spring_web.application.security.port;

import java.util.List;

public interface TokenProviderPort {

    String issueAccessToken(String subject, List<String> roles);

    String extractSubjectFromAccessToken(String token);

    String issueMfaChallengeToken(String subject);

    String extractSubjectFromMfaChallengeToken(String token);
}
