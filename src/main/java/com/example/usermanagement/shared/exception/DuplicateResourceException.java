package com.example.usermanagement.shared.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 * <p>
 * Handled by GlobalExceptionHandler to produce 409 Conflict responses.
 * <p>
 * SECURITY NOTE (SEC-01): When used for user-related duplicates, the error
 * message should be generic to prevent user enumeration. For example, during
 * registration, do NOT say "Email already registered" -- instead use a
 * generic message like "Unable to create account".
 */
public class DuplicateResourceException extends RuntimeException {

    private final String resourceType;
    private final String field;

    /**
     * Creates a new DuplicateResourceException.
     *
     * @param resourceType the type of resource (e.g., "User", "Email")
     * @param field        the field that caused the conflict (e.g., "email", "username")
     */
    public DuplicateResourceException(String resourceType, String field) {
        super(String.format("%s already exists for field: %s", resourceType, field));
        this.resourceType = resourceType;
        this.field = field;
    }

    /**
     * Creates a DuplicateResourceException with a custom message.
     * <p>
     * Use this constructor when you need a security-safe generic message.
     *
     * @param message the error message
     */
    public DuplicateResourceException(String message) {
        super(message);
        this.resourceType = null;
        this.field = null;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getField() {
        return field;
    }
}
