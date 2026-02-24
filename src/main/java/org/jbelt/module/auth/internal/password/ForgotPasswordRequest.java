package org.jbelt.module.auth.internal.password;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for requesting a password reset email.
 *
 * @param email the email address to send the reset link to
 */
@Schema(description = "Forgot password request")
public record ForgotPasswordRequest(
    @Schema(description = "Email address to send reset link to", example = "user@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    String email
) {
}
