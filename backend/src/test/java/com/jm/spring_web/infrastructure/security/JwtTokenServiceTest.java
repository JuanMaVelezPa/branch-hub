package com.jm.spring_web.infrastructure.security;

import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenServiceTest {

    @Test
    void shouldIssueAndParseToken() {
        JwtTokenService service = new JwtTokenService(new JwtProperties(
                "test-secret-test-secret-test-secret-123",
                3600,
                7200,
                "REFRESH_TOKEN",
                300));

        String token = service.issueAccessToken("admin", List.of("APP_ADMIN"));

        assertEquals("admin", service.extractSubjectFromAccessToken(token));
    }

    @Test
    void shouldFailWithInvalidToken() {
        JwtTokenService service = new JwtTokenService(new JwtProperties(
                "test-secret-test-secret-test-secret-123",
                3600,
                7200,
                "REFRESH_TOKEN",
                300));

        assertThrows(RuntimeException.class, () -> service.extractSubjectFromAccessToken("invalid-token"));
    }

    @Test
    void shouldFailWhenTokenIsExpired() throws InterruptedException {
        JwtTokenService service = new JwtTokenService(new JwtProperties(
                "test-secret-test-secret-test-secret-123",
                1,
                2,
                "REFRESH_TOKEN",
                300));
        String token = service.issueAccessToken("admin", List.of("APP_ADMIN"));

        Thread.sleep(1200);

        assertThrows(RuntimeException.class, () -> service.extractSubjectFromAccessToken(token));
    }

    @Test
    void shouldFailWithWeakSecret() {
        assertThrows(WeakKeyException.class, () -> new JwtTokenService(new JwtProperties("short-secret", 3600, 7200, "REFRESH_TOKEN", 300)));
    }

    @Test
    void shouldIssueAndParseMfaChallengeToken() {
        JwtTokenService service = new JwtTokenService(new JwtProperties(
                "test-secret-test-secret-test-secret-123",
                3600,
                7200,
                "REFRESH_TOKEN",
                300));
        String subject = "00000000-0000-0000-0000-000000000099";
        String token = service.issueMfaChallengeToken(subject);
        assertEquals(subject, service.extractSubjectFromMfaChallengeToken(token));
    }
}
