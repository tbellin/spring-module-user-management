package com.example.usermanagement.user.internal;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO for admin-initiated user profile update.
 * <p>
 * All fields are optional -- only non-null values are applied.
 *
 * @param firstName the user's first name (nullable)
 * @param lastName  the user's last name (nullable)
 * @param roles     the role names to assign (nullable; e.g., ["ROLE_USER", "ROLE_ADMIN"])
 */
@Schema(description = "Admin user update request (all optional)")
public record UpdateUserRequest(
    @Schema(description = "First name", example = "Jane")
    String firstName,
    @Schema(description = "Last name", example = "Smith")
    String lastName,
    @Schema(description = "Roles to assign", example = "[\"ROLE_USER\", \"ROLE_ADMIN\"]")
    List<String> roles
) {
}
