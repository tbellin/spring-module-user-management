package com.example.usermanagement.user.internal;

/**
 * DTO for admin-initiated user profile update.
 * <p>
 * All fields are optional -- only non-null values are applied.
 *
 * @param firstName the user's first name (nullable)
 * @param lastName  the user's last name (nullable)
 * @param role      the role name to assign (nullable; e.g., "ROLE_ADMIN")
 */
public record UpdateUserRequest(
    String firstName,
    String lastName,
    String role
) {
}
