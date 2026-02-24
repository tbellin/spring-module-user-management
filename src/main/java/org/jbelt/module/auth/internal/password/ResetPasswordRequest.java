package org.jbelt.module.auth.internal.password;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for setting a new password via reset token.
 *
 * @param token           the password reset token from the email link
 * @param newPassword     the new password (minimum 8 characters)
 * @param confirmPassword confirmation of the new password
 */
@Schema(description = "Reset password with token")
public record ResetPasswordRequest(
    @Schema(description = "Password reset token from email link", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "Token is required")
    String token,

    @Schema(description = "New password (minimum 8 characters)", example = "newpassword123")
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    String newPassword,

    @Schema(description = "Confirm new password", example = "newpassword123")
    @NotBlank(message = "Password confirmation is required")
    String confirmPassword
) {
}
