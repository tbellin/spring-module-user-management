package com.example.usermanagement.auth.internal.password;

/**
 * Sealed interface representing the possible outcomes of a password reset.
 * <p>
 * Using a sealed type ensures all cases are handled explicitly in switch
 * expressions and provides compile-time exhaustiveness checking.
 */
public sealed interface PasswordResetResult {

    /**
     * Password was successfully reset.
     */
    record Success() implements PasswordResetResult {}

    /**
     * Token has expired - user should request a new reset email.
     */
    record Expired() implements PasswordResetResult {}

    /**
     * Token is invalid (not found in database).
     */
    record Invalid() implements PasswordResetResult {}

    /**
     * Token was already used (clicked twice quickly).
     */
    record AlreadyUsed() implements PasswordResetResult {}
}
