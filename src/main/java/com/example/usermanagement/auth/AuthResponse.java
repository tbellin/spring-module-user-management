package com.example.usermanagement.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

/**
 * Response DTO returned after successful authentication.
 * <p>
 * Contains the JWT token, token metadata, and basic user information.
 * This is part of the public auth module API.
 *
 * @param token       the JWT access token
 * @param tokenType   the token type (always "Bearer")
 * @param expiresIn   token expiration time in seconds
 * @param email       the authenticated user's email
 * @param displayName the user's display name
 * @param roles       the user's roles (e.g., ROLE_USER, ROLE_ADMIN)
 */
@Schema(description = "Authentication response with JWT token")
public record AuthResponse(
    @Schema(description = "JWT access token")
    String token,
    @Schema(description = "Token type (always Bearer)", example = "Bearer")
    String tokenType,
    @Schema(description = "Token expiration in milliseconds", example = "900000")
    long expiresIn,
    @Schema(description = "Authenticated user's email", example = "user@example.com")
    String email,
    @Schema(description = "User's display name", example = "user@example.com")
    String displayName,
    @Schema(description = "User's roles", example = "[\"ROLE_USER\"]")
    Set<String> roles
) {
    /**
     * Convenience constructor that defaults tokenType to "Bearer".
     *
     * @param token       the JWT access token
     * @param expiresIn   token expiration time in seconds
     * @param email       the authenticated user's email
     * @param displayName the user's display name
     * @param roles       the user's roles
     */
    public AuthResponse(String token, long expiresIn, String email,
                        String displayName, Set<String> roles) {
        this(token, "Bearer", expiresIn, email, displayName, roles);
    }
}
