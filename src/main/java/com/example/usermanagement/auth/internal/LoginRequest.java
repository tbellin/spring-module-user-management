package com.example.usermanagement.auth.internal;

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
public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    String password,

    boolean rememberMe
) {
}
