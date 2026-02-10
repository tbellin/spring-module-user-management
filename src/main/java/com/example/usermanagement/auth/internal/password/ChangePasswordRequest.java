package com.example.usermanagement.auth.internal.password;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for authenticated password change.
 * <p>
 * Requires current password for verification (PASS-01 requirement).
 *
 * @param currentPassword the user's current password for verification
 * @param newPassword     the new password (minimum 8 characters)
 * @param confirmPassword confirmation of the new password
 */
public record ChangePasswordRequest(
    @NotBlank(message = "Current password is required")
    String currentPassword,

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    String newPassword,

    @NotBlank(message = "Password confirmation is required")
    String confirmPassword
) {
}
