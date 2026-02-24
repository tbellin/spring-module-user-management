package org.jbelt.module.user.internal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * DTO for admin-initiated user creation (invite).
 *
 * @param email     the email address of the user to invite
 * @param username  the display name / username (nullable, defaults to email)
 * @param firstName the user's first name (nullable)
 * @param lastName  the user's last name (nullable)
 * @param enabled   whether the account should be enabled (nullable, defaults to true)
 * @param roles     the roles to assign (e.g., ["ROLE_USER", "ROLE_ADMIN"])
 */
@Schema(description = "Admin user creation request")
public record CreateUserRequest(
    @Schema(description = "Email address for the new user", example = "newuser@example.com")
    @NotBlank @Email String email,
    @Schema(description = "Username (defaults to email if not provided)", example = "newuser")
    String username,
    @Schema(description = "First name", example = "Jane")
    String firstName,
    @Schema(description = "Last name", example = "Smith")
    String lastName,
    @Schema(description = "Whether account is enabled (defaults to true)", example = "true")
    Boolean enabled,
    @Schema(description = "Roles to assign", example = "[\"ROLE_USER\"]")
    @NotEmpty List<String> roles
) {
    /**
     * Returns whether the account should be enabled, defaulting to true if not specified.
     */
    public boolean isEnabled() {
        return enabled == null || enabled;
    }
}
