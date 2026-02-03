package com.example.usermanagement.shared.exception;

/**
 * Exception thrown when a requested resource is not found.
 * <p>
 * Handled by GlobalExceptionHandler to produce 404 Not Found responses.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final String identifier;

    /**
     * Creates a new ResourceNotFoundException.
     *
     * @param resourceType the type of resource (e.g., "User", "Role")
     * @param identifier   the identifier that was not found (e.g., email, id)
     */
    public ResourceNotFoundException(String resourceType, String identifier) {
        super(String.format("%s not found: %s", resourceType, identifier));
        this.resourceType = resourceType;
        this.identifier = identifier;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getIdentifier() {
        return identifier;
    }
}
