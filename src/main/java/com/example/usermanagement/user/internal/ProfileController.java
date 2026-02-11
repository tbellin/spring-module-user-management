package com.example.usermanagement.user.internal;

import com.example.usermanagement.shared.dto.UserDto;
import com.example.usermanagement.shared.exception.ResourceNotFoundException;
import com.example.usermanagement.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the authenticated user's profile.
 * <p>
 * Provides JSON endpoints for viewing and updating the current user's profile:
 * <ul>
 *   <li>GET /api/v1/users/me - returns current user profile</li>
 *   <li>PUT /api/v1/users/me - updates current user profile</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/users")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns the current authenticated user's profile.
     *
     * @param authentication the Spring Security authentication object
     * @return the user's profile as a UserDto
     * @throws ResourceNotFoundException if the user cannot be found
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        UserDto userDto = userService.getUserByEmail(authentication.getName())
            .orElseThrow(() -> new ResourceNotFoundException("User", authentication.getName()));
        return ResponseEntity.ok(userDto);
    }

    /**
     * Updates the current authenticated user's profile.
     *
     * @param authentication the Spring Security authentication object
     * @param request        the profile update fields
     * @return the updated user profile
     */
    @PutMapping("/me")
    public ResponseEntity<UserDto> updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody ProfileUpdateRequest request) {
        UserDto updatedDto = userService.updateProfile(
            authentication.getName(),
            request.displayName(),
            request.firstName(),
            request.lastName()
        );
        return ResponseEntity.ok(updatedDto);
    }
}
