package com.example.usermanagement.auth.internal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user registration.
 * <p>
 * Validates input per CONTEXT.md requirements:
 * - Email: valid format, not blank
 * - Password: minimum 8 characters, no complexity rules
 * - FirstName: 2-50 characters, required
 * - LastName: optional, max 50 characters
 *
 * @param email     the user's email address (used for login)
 * @param password  the user's password (will be hashed before storage)
 * @param firstName the user's first name
 * @param lastName  the user's last name (optional)
 */
@Schema(description = "User registration request")
public record RegistrationRequest(
    @Schema(description = "Email address (used for login)", example = "user@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @Schema(description = "Password (minimum 8 characters)", example = "password123")
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,

    @Schema(description = "First name", example = "John")
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    String firstName,

    @Schema(description = "Last name (optional)", example = "Doe")
    @Size(max = 50, message = "Last name must be at most 50 characters")
    String lastName
) {
}
