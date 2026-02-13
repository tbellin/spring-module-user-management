package com.example.usermanagement.user.internal;

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
public record CreateUserRequest(
    @NotBlank @Email String email,
    String username,
    String firstName,
    String lastName,
    Boolean enabled,
    @NotEmpty List<String> roles
) {
    /**
     * Returns whether the account should be enabled, defaulting to true if not specified.
     */
    public boolean isEnabled() {
        return enabled == null || enabled;
    }
}
