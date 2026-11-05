package dev.jotaeme.branchhub.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {
    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String MDC_TRANSACTION_ID = "transactionId";
    public static final String MDC_HTTP_METHOD = "httpMethod";
    public static final String MDC_HTTP_PATH = "httpPath";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String transactionId = resolveTransactionId(request.getHeader(CORRELATION_HEADER));

        MDC.put(MDC_TRANSACTION_ID, transactionId);
        MDC.put(MDC_HTTP_METHOD, request.getMethod());
        MDC.put(MDC_HTTP_PATH, request.getRequestURI());
        response.setHeader(CORRELATION_HEADER, transactionId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String resolveTransactionId(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return UUID.randomUUID().toString();
        }
        try {
            return UUID.fromString(headerValue).toString();
        } catch (IllegalArgumentException ignored) {
            return UUID.randomUUID().toString();
        }
    }
}
