package com.example.usermanagement.shared.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for application-level exceptions.
 * <p>
 * Produces RFC 9457 ProblemDetail JSON responses for consistent API error handling.
 * SUCCESS CRITERIA #4: API error responses follow a consistent JSON structure.
 * <p>
 * IMPORTANT: Security exceptions (401, 403) are handled by Spring Security's
 * AuthenticationEntryPoint and AccessDeniedHandler, NOT this @ControllerAdvice.
 * Security filter chain exceptions occur before reaching Spring MVC.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles ResourceNotFoundException (404 Not Found).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.debug("Resource not found: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Resource Not Found");
        problem.setDetail(ex.getMessage());

        if (ex.getResourceType() != null) {
            problem.setProperty("resourceType", ex.getResourceType());
        }

        return problem;
    }

    /**
     * Handles DuplicateResourceException (409 Conflict).
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicateResource(DuplicateResourceException ex, WebRequest request) {
        log.debug("Duplicate resource: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setTitle("Duplicate Resource");
        problem.setDetail(ex.getMessage());

        if (ex.getResourceType() != null) {
            problem.setProperty("resourceType", ex.getResourceType());
        }
        if (ex.getField() != null) {
            problem.setProperty("field", ex.getField());
        }

        return problem;
    }

    /**
     * Handles BadRequestException (400 Bad Request).
     */
    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(BadRequestException ex, WebRequest request) {
        log.debug("Bad request: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Bad Request");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    /**
     * Handles validation errors from @Valid annotations (400 Bad Request).
     * <p>
     * Overrides the default Spring MVC handling to provide field-level error details.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.debug("Validation failed: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation Failed");
        problem.setDetail("One or more fields have validation errors");

        // Collect field errors into a map
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        problem.setProperty("errors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handles IllegalArgumentException (400 Bad Request).
     * <p>
     * Catches programming errors that escape validation layers.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid Argument");
        problem.setDetail(ex.getMessage());

        return problem;
    }

    /**
     * Handles AuthenticationException (401 Unauthorized).
     * <p>
     * Catches authentication failures from AuthenticationManager.authenticate() calls
     * in controllers (e.g., login endpoint). Returns a generic message to prevent
     * user enumeration (SEC-01).
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        log.debug("Authentication failed: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Unauthorized");
        problem.setDetail("Bad credentials");

        return problem;
    }

    /**
     * Handles unexpected exceptions (500 Internal Server Error).
     * <p>
     * Logs the full stack trace but returns a generic message to avoid leaking
     * internal details to clients.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal Server Error");
        problem.setDetail("An unexpected error occurred. Please try again later.");

        return problem;
    }
}
