package com.example.usermanagement.auth.internal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user login.
 * <p>
 * Contains credentials and remember-me preference.
 *
 * @param email      the user's email address
 * @param password   the user's password
 * @param rememberMe whether to use extended token expiration
 */
@Schema(description = "Login credentials")
public record LoginRequest(
    @Schema(description = "Email address", example = "user@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @Schema(description = "Password", example = "password123")
    @NotBlank(message = "Password is required")
    String password,

    @Schema(description = "Extend token expiration to 7 days", example = "false")
    boolean rememberMe
) {
}
