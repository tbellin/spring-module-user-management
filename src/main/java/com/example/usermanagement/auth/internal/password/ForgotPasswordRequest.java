package com.example.usermanagement.auth.internal.password;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for requesting a password reset email.
 *
 * @param email the email address to send the reset link to
 */
public record ForgotPasswordRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    String email
) {
}
