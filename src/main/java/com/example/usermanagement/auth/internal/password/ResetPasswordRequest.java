package com.example.usermanagement.auth.internal.password;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for setting a new password via reset token.
 *
 * @param token           the password reset token from the email link
 * @param newPassword     the new password (minimum 8 characters)
 * @param confirmPassword confirmation of the new password
 */
public record ResetPasswordRequest(
    @NotBlank(message = "Token is required")
    String token,

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    String newPassword,

    @NotBlank(message = "Password confirmation is required")
    String confirmPassword
) {
}
