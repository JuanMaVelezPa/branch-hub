package com.jm.spring_web.infrastructure.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

final class ClientIpResolver {

    private ClientIpResolver() {
    }

    /**
     * Resolves the client IP for rate limiting. If {@code X-Forwarded-For} is present, the first
     * hop is used (typical when a single trusted reverse proxy appends the chain). Behind multiple
     * untrusted proxies this can be spoofed — configure your edge proxy accordingly.
     */
    static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = comma < 0 ? forwarded.trim() : forwarded.substring(0, comma).trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        return request.getRemoteAddr();
    }
}
