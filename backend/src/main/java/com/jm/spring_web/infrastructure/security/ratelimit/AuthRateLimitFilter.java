package com.jm.spring_web.infrastructure.security.ratelimit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Throttles {@code POST /api/v1/auth/login} (per IP and per login identifier) and {@code POST /api/v1/auth/refresh} per IP.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String REFRESH_PATH = "/api/v1/auth/refresh";

    private final AuthRateLimitProperties properties;
    private final AuthRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public AuthRateLimitFilter(
            AuthRateLimitProperties properties,
            AuthRateLimiter rateLimiter,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.enabled()) {
            return true;
        }
        return !isRateLimitedEndpoint(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = pathWithoutContext(request);
        HttpServletRequest requestForChain = request;
        Optional<String> loginIdentifier = Optional.empty();
        if (LOGIN_PATH.equals(path)) {
            CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request, 65_536);
            loginIdentifier = parseNormalizedLoginIdentifier(wrapped.cachedBody());
            requestForChain = wrapped;
        }

        String ip = ClientIpResolver.resolve(request);
        boolean allowed = LOGIN_PATH.equals(path)
                ? rateLimiter.tryLogin(ip, loginIdentifier)
                : rateLimiter.tryRefresh(ip);

        if (allowed) {
            filterChain.doFilter(requestForChain, response);
            return;
        }

        writeTooManyRequests(response, request);
    }

    private Optional<String> parseNormalizedLoginIdentifier(byte[] body) {
        if (body == null || body.length == 0) {
            return Optional.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root == null || root.get("username") == null || root.get("username").isNull()) {
                return Optional.empty();
            }
            String u = root.get("username").asText("").trim().toLowerCase(Locale.ROOT);
            return u.isEmpty() ? Optional.empty() : Optional.of(u);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /**
     * Buffer the JSON body so the filter can read it for rate-limit keys and the controller still sees the same bytes.
     */
    private static final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyHttpServletRequest(HttpServletRequest request, int maxBytes) throws IOException {
            super(request);
            byte[] read = request.getInputStream().readAllBytes();
            if (read.length > maxBytes) {
                this.body = new byte[0];
            } else {
                this.body = read;
            }
        }

        byte[] cachedBody() {
            return body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream source = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return source.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }

                @Override
                public int read() {
                    return source.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }

    private static boolean isRateLimitedEndpoint(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = pathWithoutContext(request);
        return LOGIN_PATH.equals(path) || REFRESH_PATH.equals(path);
    }

    private static String pathWithoutContext(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && uri.startsWith(context)) {
            return uri.substring(context.length());
        }
        return uri;
    }

    private void writeTooManyRequests(HttpServletResponse response, HttpServletRequest request) throws IOException {
        int status = HttpStatus.TOO_MANY_REQUESTS.value();
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", URI.create("https://api.spring-web/errors/429").toString());
        body.put("title", "Too Many Requests");
        body.put("status", status);
        body.put("detail", "Too many authentication requests from this client. Try again later.");
        body.put("instance", request.getRequestURI());
        body.put("timestamp", Instant.now());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
