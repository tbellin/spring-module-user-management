package org.jbelt.module.auth.internal.verification;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Web controller for email verification link handling.
 * <p>
 * Handles the verification links sent in emails. Per CONTEXT.md:
 * - One-click activation (link click auto-activates)
 * - After success: stay on success page with login link (no auto-redirect/login)
 * - Error messages: specific (distinguish expired vs invalid vs already verified)
 */
@Controller
public class EmailVerificationController {

    private final VerificationService verificationService;

    public EmailVerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    /**
     * Handles email verification link clicks.
     * <p>
     * Token is in the path (not query parameter) for security:
     * - Less likely to be logged in access logs
     * - Better cache control
     *
     * @param token the verification token from the email link
     * @param model the Spring MVC model
     * @return the view name (success or error page)
     */
    @GetMapping("/verify/{token}")
    public String verifyEmail(@PathVariable String token, Model model) {
        VerificationResult result = verificationService.verifyToken(token);

        return switch (result) {
            case VerificationResult.Success() -> {
                model.addAttribute("message", "Your email has been verified successfully!");
                model.addAttribute("subtitle", "You can now log in to your account.");
                yield "auth/verify-success";
            }
            case VerificationResult.AlreadyVerified() -> {
                model.addAttribute("message", "Email Already Verified");
                model.addAttribute("subtitle", "This email address has already been verified.");
                yield "auth/verify-success";
            }
            case VerificationResult.Expired() -> {
                model.addAttribute("errorTitle", "Verification Link Expired");
                model.addAttribute("errorMessage", "This verification link has expired. Please request a new verification email.");
                model.addAttribute("showResendLink", true);
                yield "auth/verify-error";
            }
            case VerificationResult.Invalid() -> {
                model.addAttribute("errorTitle", "Invalid Verification Link");
                model.addAttribute("errorMessage", "This verification link is invalid or has been tampered with.");
                model.addAttribute("showResendLink", false);
                yield "auth/verify-error";
            }
            case VerificationResult.AlreadyUsed() -> {
                model.addAttribute("errorTitle", "Link Already Used");
                model.addAttribute("errorMessage", "This verification link has already been used. If you need to verify your email, please request a new link.");
                model.addAttribute("showResendLink", true);
                yield "auth/verify-error";
            }
        };
    }
}
