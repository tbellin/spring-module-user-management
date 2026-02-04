package com.example.usermanagement.auth.internal;

import com.example.usermanagement.shared.dto.Toast;
import com.example.usermanagement.shared.exception.DuplicateResourceException;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final AuthenticationManager authenticationManager;

    public AuthWebController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
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
     * On success: creates user, auto-logs in, redirects to home with success toast.
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
            // Create the user
            authService.registerUser(form.getEmail(), form.getPassword(), form.getDisplayName());

            // Auto-login after registration using AuthenticationManager
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(form.getEmail(), form.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Success toast
            redirectAttributes.addFlashAttribute("toast",
                new Toast("success", "Welcome! Your account has been created."));

            return "redirect:/";

        } catch (DuplicateResourceException e) {
            result.rejectValue("email", "duplicate", "An account with this email already exists");
            return "auth/register";
        }
    }
}
