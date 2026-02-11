package com.example.usermanagement.auth.internal.password;

import com.example.usermanagement.auth.internal.verification.ResendRateLimiter;
import com.example.usermanagement.shared.dto.Toast;
import com.example.usermanagement.shared.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Web controller for Thymeleaf password management pages.
 * <p>
 * Provides form-based password operations:
 * <ul>
 *   <li>GET/POST /change-password - Change password (authenticated)</li>
 *   <li>GET/POST /forgot-password - Request password reset (public)</li>
 *   <li>GET/POST /reset-password - Reset password with token (public)</li>
 * </ul>
 * <p>
 * After password change: session is invalidated, user is redirected to login.
 * After password reset: success page with login button (no auto-login).
 * On error: error page with option to request new reset link.
 */
@Controller
public class PasswordWebController {

    private final PasswordService passwordService;
    private final ResendRateLimiter rateLimiter;

    public PasswordWebController(PasswordService passwordService, ResendRateLimiter rateLimiter) {
        this.passwordService = passwordService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Shows the change password form.
     * <p>
     * Requires authentication (enforced by SecurityConfig anyRequest().authenticated()).
     *
     * @return the change password view
     */
    @GetMapping("/change-password")
    public String showChangePasswordForm() {
        return "auth/change-password";
    }

    /**
     * Processes the change password form submission.
     * <p>
     * On success: clears SecurityContext, invalidates session, redirects to login
     * with a success toast message. User must re-login with new password.
     * On error: returns to form with error message.
     *
     * @param currentPassword    the user's current password
     * @param newPassword        the new password
     * @param confirmPassword    confirmation of the new password
     * @param authentication     the authenticated user
     * @param request            the HTTP request (for session invalidation)
     * @param redirectAttributes for flash messages on redirect
     * @param model              the Spring MVC model
     * @return view name or redirect
     */
    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes,
            Model model) {

        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "New password and confirmation do not match.");
            return "auth/change-password";
        }

        // Validate minimum length
        if (newPassword.length() < 8) {
            model.addAttribute("error", "New password must be at least 8 characters.");
            return "auth/change-password";
        }

        try {
            passwordService.changePassword(authentication.getName(), currentPassword, newPassword);
        } catch (BadRequestException e) {
            model.addAttribute("error", "Current password is incorrect.");
            return "auth/change-password";
        }

        // Invalidate session and clear security context (force re-login)
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        redirectAttributes.addFlashAttribute("toast",
            new Toast("success", "Password changed successfully. Please log in with your new password."));

        return "redirect:/login";
    }

    /**
     * Shows the forgot password form.
     * <p>
     * Public endpoint (permitAll in SecurityConfig).
     *
     * @return the forgot password view
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "auth/forgot-password";
    }

    /**
     * Processes the forgot password form submission.
     * <p>
     * SEC-01 compliance: Always shows the same message regardless of whether
     * the email exists. Rate limited to prevent abuse.
     *
     * @param email the email address to send the reset link to
     * @param model the Spring MVC model
     * @return the forgot password view (stays on same page with message)
     */
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email, Model model) {
        if (!rateLimiter.canResend(email)) {
            model.addAttribute("error", "Please wait before requesting another reset email.");
            return "auth/forgot-password";
        }

        passwordService.requestPasswordReset(email);
        rateLimiter.recordResend(email);

        model.addAttribute("message",
            "If an account exists with this email, we sent a reset link.");
        return "auth/forgot-password";
    }

    /**
     * Shows the reset password form.
     * <p>
     * Public endpoint (permitAll in SecurityConfig). The token is passed as a
     * query parameter and added to the model for the form's hidden field.
     *
     * @param token the password reset token from the email link
     * @param model the Spring MVC model
     * @return the reset password view
     */
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    /**
     * Processes the password reset form submission.
     * <p>
     * On success: shows success page with login button (no auto-login).
     * On error: shows error page with option to request new reset link.
     *
     * @param token           the password reset token
     * @param newPassword     the new password
     * @param confirmPassword confirmation of the new password
     * @param model           the Spring MVC model
     * @return view name (success or error page)
     */
    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model) {

        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "New password and confirmation do not match.");
            model.addAttribute("token", token);
            return "auth/reset-password";
        }

        // Validate minimum length
        if (newPassword.length() < 8) {
            model.addAttribute("error", "New password must be at least 8 characters.");
            model.addAttribute("token", token);
            return "auth/reset-password";
        }

        PasswordResetResult result = passwordService.resetPassword(token, newPassword);

        return switch (result) {
            case PasswordResetResult.Success() -> "auth/reset-success";
            case PasswordResetResult.Expired() -> {
                model.addAttribute("error", "This reset link has expired. Please request a new one.");
                yield "auth/reset-error";
            }
            case PasswordResetResult.Invalid() -> {
                model.addAttribute("error", "Invalid reset link.");
                yield "auth/reset-error";
            }
            case PasswordResetResult.AlreadyUsed() -> {
                model.addAttribute("error", "This reset link has already been used.");
                yield "auth/reset-error";
            }
        };
    }
}
