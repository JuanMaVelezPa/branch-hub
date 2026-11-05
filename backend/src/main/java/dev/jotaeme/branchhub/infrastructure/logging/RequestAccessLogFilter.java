package dev.jotaeme.branchhub.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestAccessLogFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestAccessLogFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return requestUri.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String requestPath = request.getRequestURI();

        try {
            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            long elapsedMs = System.currentTimeMillis() - start;
            LOG.error(
                    "Request failed method={} path={} status={} elapsedMs={} transactionId={}",
                    method,
                    requestPath,
                    response.getStatus(),
                    elapsedMs,
                    MDC.get(RequestCorrelationFilter.MDC_TRANSACTION_ID),
                    exception
            );
            if (exception instanceof ServletException servletException) {
                throw servletException;
            }
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
            throw (RuntimeException) exception;
        } finally {
            long elapsedMs = System.currentTimeMillis() - start;
            int status = response.getStatus();
            if (status >= 500) {
                LOG.error(
                        "Request completed method={} path={} status={} elapsedMs={} transactionId={}",
                        method,
                        requestPath,
                        status,
                        elapsedMs,
                        MDC.get(RequestCorrelationFilter.MDC_TRANSACTION_ID)
                );
            } else {
                LOG.info(
                        "Request completed method={} path={} status={} elapsedMs={} transactionId={}",
                        method,
                        requestPath,
                        status,
                        elapsedMs,
                        MDC.get(RequestCorrelationFilter.MDC_TRANSACTION_ID)
                );
            }
        }
    }
}
