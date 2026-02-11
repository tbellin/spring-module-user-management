package com.example.usermanagement.user.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for admin-initiated user creation (invite).
 *
 * @param email the email address of the user to invite
 * @param role  the role to assign (e.g., "ROLE_USER", "ROLE_ADMIN")
 */
public record CreateUserRequest(
    @NotBlank @Email String email,
    @NotBlank String role
) {
}
