package org.jbelt.module.auth.internal.verification;

/**
 * Sealed interface representing the possible outcomes of email verification.
 * <p>
 * Using a sealed type ensures all cases are handled explicitly in switch
 * expressions and provides compile-time exhaustiveness checking.
 */
public sealed interface VerificationResult {

    /**
     * Verification succeeded - account is now verified.
     */
    record Success() implements VerificationResult {}

    /**
     * Account was already verified (token may have been reused).
     */
    record AlreadyVerified() implements VerificationResult {}

    /**
     * Token has expired - user should request a new verification email.
     */
    record Expired() implements VerificationResult {}

    /**
     * Token is invalid (not found in database).
     */
    record Invalid() implements VerificationResult {}

    /**
     * Token was already used (clicked twice quickly).
     */
    record AlreadyUsed() implements VerificationResult {}
}
