package com.example.usermanagement.auth.internal.password;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Change password request")
public record ChangePasswordRequest(
    @Schema(description = "Current password for verification", example = "oldpassword123")
    @NotBlank(message = "Current password is required")
    String currentPassword,

    @Schema(description = "New password (minimum 8 characters)", example = "newpassword123")
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    String newPassword,

    @Schema(description = "Confirm new password (must match newPassword)", example = "newpassword123")
    @NotBlank(message = "Password confirmation is required")
    String confirmPassword
) {
}
