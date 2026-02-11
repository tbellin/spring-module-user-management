package com.example.usermanagement.user.internal;

import com.example.usermanagement.shared.dto.Toast;
import com.example.usermanagement.shared.dto.UserDto;
import com.example.usermanagement.shared.exception.ResourceNotFoundException;
import com.example.usermanagement.user.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Thymeleaf controller for the user profile page.
 * <p>
 * Provides form-based profile viewing and editing:
 * <ul>
 *   <li>GET /profile - display current user's profile</li>
 *   <li>POST /profile - update profile and redirect back with toast</li>
 * </ul>
 */
@Controller
public class ProfileWebController {

    private final UserService userService;

    public ProfileWebController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Displays the current user's profile page.
     *
     * @param authentication the Spring Security authentication object
     * @param model          the Spring MVC model
     * @return the profile view name
     */
    @GetMapping("/profile")
    public String showProfile(Authentication authentication, Model model) {
        UserDto user = userService.getUserByEmail(authentication.getName())
            .orElseThrow(() -> new ResourceNotFoundException("User", authentication.getName()));
        model.addAttribute("user", user);
        return "profile/profile";
    }

    /**
     * Updates the current user's profile and redirects back with a success toast.
     *
     * @param authentication     the Spring Security authentication object
     * @param displayName        the new display name
     * @param firstName          the new first name (optional)
     * @param lastName           the new last name (optional)
     * @param redirectAttributes for flash messages on redirect
     * @return redirect to the profile page
     */
    @PostMapping("/profile")
    public String updateProfile(
            Authentication authentication,
            @RequestParam String displayName,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            RedirectAttributes redirectAttributes) {

        userService.updateProfile(authentication.getName(), displayName, firstName, lastName);

        redirectAttributes.addFlashAttribute("toast",
            new Toast("success", "Profile updated successfully."));

        return "redirect:/profile";
    }
}
