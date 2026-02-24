package org.jbelt.module.auth.internal.password;

import org.jbelt.module.auth.internal.verification.ResendRateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for password management endpoints.
 * <p>
 * Provides stateless JWT-authenticated and public endpoints for:
 * <ul>
 *   <li>POST /api/v1/auth/change-password - Change password (requires authentication)</li>
 *   <li>POST /api/v1/auth/forgot-password - Request password reset email (public, SEC-01)</li>
 *   <li>POST /api/v1/auth/reset-password - Reset password with token (public)</li>
 * </ul>
 * <p>
 * Error handling is delegated to GlobalExceptionHandler:
 * <ul>
 *   <li>BadRequestException (400) - wrong current password, validation errors</li>
 *   <li>MethodArgumentNotValidException (400) - bean validation errors</li>
 * </ul>
 */
@Tag(name = "Password Management", description = "Change password, forgot password, and reset password flows")
@RestController
@RequestMapping("/api/v1/auth")
public class PasswordController {

    private final PasswordService passwordService;
    private final ResendRateLimiter rateLimiter;

    public PasswordController(PasswordService passwordService, ResendRateLimiter rateLimiter) {
        this.passwordService = passwordService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Changes the password for the authenticated user.
     * <p>
     * Requires a valid JWT token. The current password must be provided
     * for verification before the new password is set.
     * <p>
     * Note: This endpoint is under /api/v1/auth/** which is permitAll in the
     * API filter chain. Authentication is enforced by checking the Authentication
     * parameter -- if no JWT is provided, authentication will be null.
     *
     * @param request        the change password request (currentPassword, newPassword, confirmPassword)
     * @param authentication the authenticated user's security context
     * @return 200 on success, 401 if not authenticated, 400 if passwords don't match or current password wrong
     */
    @Operation(summary = "Change password",
              description = "Changes the authenticated user's password. Requires current password for verification.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password changed"),
        @ApiResponse(responseCode = "400", description = "Wrong current password or validation error"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        // /api/v1/auth/** is permitAll, so authentication may be null
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Authentication required"));
        }

        // Validate confirmPassword matches newPassword
        if (!request.newPassword().equals(request.confirmPassword())) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "New password and confirmation do not match"));
        }

        // Service validates current password and sets new one
        passwordService.changePassword(authentication.getName(), request.currentPassword(), request.newPassword());

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    /**
     * Requests a password reset email.
     * <p>
     * SEC-01 compliance: Always returns the same response regardless of whether
     * the email exists, is verified, or is unknown. This prevents user enumeration.
     * <p>
     * Rate limited: 60 second cooldown between requests per email address.
     *
     * @param request the forgot password request containing the email address
     * @return 200 with generic message, or 429 if rate limited
     */
    @Operation(summary = "Request password reset",
              description = "Sends a password reset email. Returns the same response regardless of whether the email exists (SEC-01).")
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Request processed"),
        @ApiResponse(responseCode = "429", description = "Rate limited")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        String email = request.email();

        // Check rate limit
        if (!rateLimiter.canResend(email)) {
            long remaining = rateLimiter.getRemainingCooldownSeconds(email);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                    "message", "Please wait before requesting another reset email",
                    "retryAfter", String.valueOf(remaining)
                ));
        }

        // Send reset email (if account exists and is verified)
        // SEC-01: Same response regardless of outcome
        passwordService.requestPasswordReset(email);
        rateLimiter.recordResend(email);

        return ResponseEntity.ok(Map.of(
            "message", "If an account exists with this email, a password reset link has been sent"
        ));
    }

    /**
     * Resets a user's password using a reset token.
     * <p>
     * Validates the token and sets the new password. The token must be valid,
     * not expired, and not previously used.
     *
     * @param request the reset password request (token, newPassword, confirmPassword)
     * @return 200 on success, 400 on expired/invalid/used token or password mismatch
     */
    @Operation(summary = "Reset password with token",
              description = "Sets a new password using the token from the reset email. Token must be valid and unused.")
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset successful"),
        @ApiResponse(responseCode = "400", description = "Invalid, expired, or used token")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        // Validate confirmPassword matches newPassword
        if (!request.newPassword().equals(request.confirmPassword())) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "New password and confirmation do not match"));
        }

        PasswordResetResult result = passwordService.resetPassword(request.token(), request.newPassword());

        return switch (result) {
            case PasswordResetResult.Success() -> ResponseEntity.ok(
                Map.of("message", "Password has been reset successfully"));
            case PasswordResetResult.Expired() -> ResponseEntity.badRequest().body(
                Map.of("message", "Reset link has expired. Please request a new one."));
            case PasswordResetResult.Invalid() -> ResponseEntity.badRequest().body(
                Map.of("message", "Invalid reset link."));
            case PasswordResetResult.AlreadyUsed() -> ResponseEntity.badRequest().body(
                Map.of("message", "This reset link has already been used."));
        };
    }
}
