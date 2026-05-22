package com.jm.spring_web.infrastructure.security.ratelimit;

import java.util.Optional;

public interface AuthRateLimiter {

    /**
     * Login is limited per client IP and, when present, per normalized login identifier (username/email in body).
     */
    boolean tryLogin(String clientIp, Optional<String> normalizedLoginIdentifier);

    boolean tryRefresh(String clientIp);
}
