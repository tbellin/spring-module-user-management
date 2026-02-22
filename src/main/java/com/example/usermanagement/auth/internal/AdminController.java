package com.example.usermanagement.auth.internal;

import com.example.usermanagement.shared.dto.UserDto;
import com.example.usermanagement.shared.exception.BadRequestException;
import com.example.usermanagement.shared.exception.ResourceNotFoundException;
import com.example.usermanagement.user.UserService;
import com.example.usermanagement.user.internal.CreateUserRequest;
import com.example.usermanagement.user.internal.UpdateUserRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST API controller for admin user management operations.
 * <p>
 * Provides CRUD endpoints at {@code /api/v1/admin/users} for:
 * <ul>
 *   <li>Listing users with pagination, search, and filtering</li>
 *   <li>Creating users via admin invite</li>
 *   <li>Updating user profile and role</li>
 *   <li>Toggling user enabled/disabled status</li>
 * </ul>
 * <p>
 * All endpoints require ADMIN role (enforced by SecurityConfig).
 * Placed in {@code auth.internal} to access both {@link UserService}
 * and {@link AdminInviteService} without violating module boundaries.
 */
@Tag(name = "Admin User Management", description = "CRUD operations for user accounts (requires ADMIN role)")
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminController {

    private final UserService userService;
    private final AdminInviteService adminInviteService;

    public AdminController(UserService userService, AdminInviteService adminInviteService) {
        this.userService = userService;
        this.adminInviteService = adminInviteService;
    }

    /**
     * Lists users with optional search, role filter, status filter, and pagination.
     *
     * @param search   partial text to match against email, first name, or last name
     * @param role     role name to filter by (e.g., "ROLE_ADMIN")
     * @param status   account status: "active" or "disabled"
     * @param pageable pagination and sorting parameters (default: 10 per page, sorted by createdAt DESC)
     * @return a page of matching users
     */
    @Operation(summary = "List users",
              description = "Lists users with optional search, role filter, status filter, and pagination. Requires ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User list"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not an admin")
    })
    @GetMapping
    public ResponseEntity<Page<UserDto>> listUsers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<UserDto> page = userService.findUsers(search, role, status, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Creates a new user via admin invite.
     * <p>
     * The user receives an invite email with a set-password link.
     *
     * @param request the create user request containing email and role
     * @return the created user with HTTP 201 status
     */
    @Operation(summary = "Create user via invite",
              description = "Creates a user account and sends an invite email with a set-password link. Requires ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @PostMapping
    public ResponseEntity<AdminInviteService.InviteResult> createUser(@Valid @RequestBody CreateUserRequest request) {
        AdminInviteService.InviteResult result = adminInviteService.inviteUser(
            request.email(), request.username(), request.firstName(), request.lastName(),
            request.isEnabled(), request.roles());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Updates a user's name and/or role.
     *
     * @param id      the user's ID
     * @param request the update request with optional firstName, lastName, and role
     * @return the updated user
     */
    @Operation(summary = "Update user",
              description = "Updates a user's name and/or roles. Requires ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {

        UserDto updated = userService.updateUser(id, request.firstName(), request.lastName(), request.roles());
        return ResponseEntity.ok(updated);
    }

    /**
     * Toggles a user's enabled/disabled status.
     * <p>
     * Prevents admins from disabling their own account.
     *
     * @param id             the user's ID
     * @param body           map containing "enabled" boolean value
     * @param authentication the current admin's authentication
     * @return the updated user
     * @throws BadRequestException if the admin attempts to disable their own account
     */
    @Operation(summary = "Toggle user status",
              description = "Enables or disables a user account. Cannot disable your own account. Requires ADMIN role.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "400", description = "Cannot disable own account"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserDto> toggleUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            @Parameter(hidden = true) Authentication authentication) {

        // Self-disable prevention
        UserDto targetUser = userService.getUserById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));

        if (targetUser.email().equals(authentication.getName())) {
            throw new BadRequestException("Cannot disable your own account");
        }

        boolean enabled = body.get("enabled");
        UserDto updated = userService.toggleUserEnabled(id, enabled);
        return ResponseEntity.ok(updated);
    }
}
