package com.example.usermanagement.auth.internal;

import com.example.usermanagement.auth.internal.verification.ResendRateLimiter;
import com.example.usermanagement.shared.dto.Toast;
import com.example.usermanagement.shared.exception.DuplicateResourceException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Web controller for Thymeleaf authentication pages.
 * <p>
 * Handles registration form submission. Login/logout are handled by Spring Security.
 * The login form posts directly to Spring Security's /login endpoint.
 */
@Controller
public class AuthWebController {

    private final AuthService authService;
    private final ResendRateLimiter rateLimiter;

    public AuthWebController(AuthService authService,
                             ResendRateLimiter rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Display login page.
     * <p>
     * Spring Security handles the actual POST /login submission.
     * Error and logout parameters are handled by the template directly.
     *
     * @return the login view name
     */
    @GetMapping("/login")
    public String showLoginForm() {
        return "auth/login";
    }

    /**
     * Display registration page with empty form.
     *
     * @param model the Spring MVC model
     * @return the register view name
     */
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "auth/register";
    }

    /**
     * Process registration form submission.
     * <p>
     * On success: creates user, sends verification email, redirects to login with
     * a message to check email. No auto-login since the user must verify first.
     * On validation error: returns to form with errors displayed.
     * On duplicate email: returns to form with email error.
     *
     * @param form               the validated registration form
     * @param result             validation result (MUST immediately follow @Valid parameter)
     * @param redirectAttributes for flash messages on redirect
     * @return view name or redirect
     */
    @PostMapping("/register")
    public String processRegistration(
            @Valid @ModelAttribute("registrationForm") RegistrationForm form,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            // Create the user (sends verification email automatically)
            authService.registerUser(form.getEmail(), form.getPassword(), form.getFirstName(), form.getLastName());

            // Redirect to login with verification notice (no auto-login - user must verify first)
            redirectAttributes.addFlashAttribute("toast",
                new Toast("success", "Registration successful! Please check your email to verify your account."));

            return "redirect:/login";

        } catch (DuplicateResourceException e) {
            result.rejectValue("email", "duplicate", "An account with this email already exists");
            return "auth/register";
        }
    }

    /**
     * Shows the resend verification email form.
     *
     * @return the resend verification view name
     */
    @GetMapping("/auth/resend-verification")
    public String showResendVerificationForm() {
        return "auth/resend-verification";
    }

    /**
     * Handles resend verification form submission.
     * <p>
     * SEC-01 compliance: Returns the same message regardless of whether
     * the account exists, is already verified, or doesn't exist.
     * Rate limiting prevents abuse of the resend functionality.
     *
     * @param email the email address to resend verification to
     * @param model the Spring MVC model
     * @return the resend verification view name
     */
    @PostMapping("/auth/resend-verification")
    public String resendVerification(@RequestParam String email, Model model) {
        if (!rateLimiter.canResend(email)) {
            long remaining = rateLimiter.getRemainingCooldownSeconds(email);
            model.addAttribute("error",
                    "Please wait " + remaining + " seconds before requesting another email.");
            return "auth/resend-verification";
        }

        authService.sendVerificationEmail(email);
        rateLimiter.recordResend(email);

        model.addAttribute("message",
                "If an account exists with this email, a verification link has been sent.");
        return "auth/resend-verification";
    }
}
