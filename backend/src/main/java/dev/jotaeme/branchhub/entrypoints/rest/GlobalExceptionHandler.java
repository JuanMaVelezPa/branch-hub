package dev.jotaeme.branchhub.entrypoints.rest;

import dev.jotaeme.branchhub.application.common.exception.ConflictException;
import dev.jotaeme.branchhub.application.common.exception.NotFoundException;
import dev.jotaeme.branchhub.application.common.exception.UnauthorizedException;
import dev.jotaeme.branchhub.application.common.exception.UnprocessableEntityException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String GENERIC_INTERNAL_ERROR = "Unexpected internal error";

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(NotFoundException exception, HttpServletRequest request) {
        log4xx(HttpStatus.NOT_FOUND, request, exception.getMessage());
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ProblemDetail> handleConflict(ConflictException exception, HttpServletRequest request) {
        log4xx(HttpStatus.CONFLICT, request, exception.getMessage());
        return build(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler({UnprocessableEntityException.class, BadCredentialsException.class})
    ResponseEntity<ProblemDetail> handleUnprocessable(RuntimeException exception, HttpServletRequest request) {
        log4xx(HttpStatus.UNPROCESSABLE_CONTENT, request, exception.getMessage());
        return build(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ProblemDetail> handleUnauthorized(UnauthorizedException exception, HttpServletRequest request) {
        log4xx(HttpStatus.UNAUTHORIZED, request, exception.getMessage());
        return build(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    ResponseEntity<ProblemDetail> handleBadRequest(Exception exception, HttpServletRequest request) {
        List<Map<String, String>> errors = List.of();
        String message;
        if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            errors = methodArgumentNotValidException.getBindingResult().getFieldErrors().stream()
                    .map(this::mapFieldError)
                    .toList();
            message = "Validation failed for one or more fields";
        } else if (exception instanceof ConstraintViolationException constraintViolationException) {
            errors = constraintViolationException.getConstraintViolations().stream()
                    .map(this::mapConstraintViolation)
                    .toList();
            message = "Validation failed for one or more constraints";
        } else {
            message = exception.getMessage() == null ? "Bad request" : exception.getMessage();
        }
        log4xx(HttpStatus.BAD_REQUEST, request, message);
        return build(HttpStatus.BAD_REQUEST, message, request, errors);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnhandled(Exception exception, HttpServletRequest request) {
        logger.error(
                "Unhandled exception status={} path={} method={} transactionId={}",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI(),
                request.getMethod(),
                MDC.get("transactionId"),
                exception
        );
        return build(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_INTERNAL_ERROR, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException exception, HttpServletRequest request) {
        log4xx(HttpStatus.NOT_FOUND, request, exception.getMessage());
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String message, HttpServletRequest request) {
        return build(status, message, request, List.of());
    }

    private ResponseEntity<ProblemDetail> build(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            List<Map<String, String>> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setType(URI.create("https://api.branch-hub/errors/" + status.value()));
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

        String transactionId = MDC.get("transactionId");
        if (transactionId != null && !transactionId.isBlank()) {
            problem.setProperty("transactionId", transactionId);
        }
        if (!errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }

        return ResponseEntity.status(status).body(problem);
    }

    private void log4xx(HttpStatus status, HttpServletRequest request, String message) {
        String safeMessage = sanitizeForLog(message);
        logger.warn(
                "Handled exception status={} path={} method={} transactionId={} message={}",
                status.value(),
                request.getRequestURI(),
                request.getMethod(),
                MDC.get("transactionId"),
                safeMessage
        );
    }

    private String sanitizeForLog(String message) {
        if (message == null) {
            return "n/a";
        }
        String normalized = message.replace('\n', ' ').replace('\r', ' ');
        return normalized.length() > 300 ? normalized.substring(0, 300) + "..." : normalized;
    }

    private Map<String, String> mapFieldError(FieldError fieldError) {
        return Map.of(
                "field", fieldError.getField(),
                "message", fieldError.getDefaultMessage() == null ? "Invalid value" : fieldError.getDefaultMessage());
    }

    private Map<String, String> mapConstraintViolation(ConstraintViolation<?> violation) {
        return Map.of(
                "field", violation.getPropertyPath().toString(),
                "message", violation.getMessage());
    }
}
