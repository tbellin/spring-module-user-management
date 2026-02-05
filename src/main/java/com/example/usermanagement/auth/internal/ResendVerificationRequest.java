package com.example.usermanagement.auth.internal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for resending verification email.
 *
 * @param email the email address to resend verification to
 */
public record ResendVerificationRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email
) {
}
