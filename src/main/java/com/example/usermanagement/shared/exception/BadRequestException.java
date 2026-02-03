package com.example.usermanagement.shared.exception;

/**
 * Exception thrown when a request is invalid or malformed.
 * <p>
 * Handled by GlobalExceptionHandler to produce 400 Bad Request responses.
 * Use this for business logic validation failures that aren't covered by
 * Jakarta Bean Validation.
 */
public class BadRequestException extends RuntimeException {

    /**
     * Creates a new BadRequestException.
     *
     * @param message description of what was wrong with the request
     */
    public BadRequestException(String message) {
        super(message);
    }

    /**
     * Creates a new BadRequestException with a cause.
     *
     * @param message description of what was wrong with the request
     * @param cause   the underlying cause
     */
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
