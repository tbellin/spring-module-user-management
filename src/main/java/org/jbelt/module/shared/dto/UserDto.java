package org.jbelt.module.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Data transfer object for user information.
 * <p>
 * This record is used to transfer user data across module boundaries
 * without exposing JPA entities.
 *
 * @param id            the user's unique identifier
 * @param email         the user's email address
 * @param username      the user's username
 * @param firstName     the user's first name (may be null)
 * @param lastName      the user's last name (may be null)
 * @param enabled       whether the user account is enabled
 * @param emailVerified whether the user's email has been verified
 * @param roles         the user's role names (e.g., "ROLE_USER", "ROLE_ADMIN")
 * @param createdAt     when the user account was created
 */
@Schema(description = "User profile data")
public record UserDto(
    @Schema(description = "User ID", example = "1")
    Long id,
    @Schema(description = "Email address", example = "user@example.com")
    String email,
    @Schema(description = "Username", example = "user@example.com")
    String username,
    @Schema(description = "First name", example = "John")
    String firstName,
    @Schema(description = "Last name", example = "Doe")
    String lastName,
    @Schema(description = "Whether account is enabled", example = "true")
    boolean enabled,
    @Schema(description = "Whether email is verified", example = "true")
    boolean emailVerified,
    @Schema(description = "Assigned roles", example = "[\"ROLE_USER\"]")
    Set<String> roles,
    @Schema(description = "Account creation timestamp", example = "2026-01-15T10:30:00")
    LocalDateTime createdAt
) {

    /**
     * Creates a UserDto from individual field values.
     * <p>
     * This factory method accepts individual values rather than an entity
     * to avoid leaking entity types across module boundaries.
     *
     * @param id            the user's unique identifier
     * @param email         the user's email address
     * @param username      the user's username
     * @param firstName     the user's first name
     * @param lastName      the user's last name
     * @param enabled       whether the user account is enabled
     * @param emailVerified whether the user's email has been verified
     * @param roles         the user's role names
     * @param createdAt     when the user account was created
     * @return a new UserDto instance
     */
    public static UserDto from(
        Long id,
        String email,
        String username,
        String firstName,
        String lastName,
        boolean enabled,
        boolean emailVerified,
        Set<String> roles,
        LocalDateTime createdAt
    ) {
        return new UserDto(id, email, username, firstName, lastName, enabled, emailVerified, roles, createdAt);
    }
}
