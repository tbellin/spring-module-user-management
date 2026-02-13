package com.example.usermanagement.user;

import com.example.usermanagement.shared.dto.UserDto;
import com.example.usermanagement.shared.exception.BadRequestException;
import com.example.usermanagement.shared.exception.ResourceNotFoundException;
import com.example.usermanagement.user.internal.AppRole;
import com.example.usermanagement.user.internal.AppUser;
import com.example.usermanagement.user.internal.RoleRepository;
import com.example.usermanagement.user.internal.UserRepository;
import com.example.usermanagement.user.internal.UserSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Public API for the user module.
 * <p>
 * This service is the only public entry point into the user module.
 * Other modules (especially auth) should use this service to access
 * user data rather than accessing repositories or entities directly.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Retrieves a user by their email address.
     *
     * @param email the email address to search for
     * @return an Optional containing the UserDto if found
     */
    public Optional<UserDto> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .map(this::toUserDto);
    }

    /**
     * Retrieves user authentication details by email address.
     * <p>
     * This method is intended for use by the auth module during login.
     * It returns the password hash and role information needed for authentication.
     *
     * @param email the email address to search for
     * @return an Optional containing the UserAuthDto if found
     */
    public Optional<UserAuthDto> getUserAuthDetailsByEmail(String email) {
        return userRepository.findByEmail(email)
            .map(this::toUserAuthDto);
    }

    /**
     * Creates a new user with the default ROLE_USER role.
     * <p>
     * The user is created with enabled=true. Email verification will
     * be handled separately in Phase 4.
     *
     * @param email        the user's email address
     * @param username     the user's username
     * @param passwordHash the pre-hashed password
     * @param firstName    the user's first name (may be null)
     * @param lastName     the user's last name (may be null)
     * @return the created user as a UserDto
     * @throws IllegalStateException if the default role (ROLE_USER) is not found
     */
    @Transactional
    public UserDto createUser(String email, String username, String passwordHash,
                              String firstName, String lastName) {
        AppRole defaultRole = roleRepository.findByName(DEFAULT_ROLE)
            .orElseThrow(() -> new IllegalStateException(
                "Default role '" + DEFAULT_ROLE + "' not found. " +
                "Ensure Flyway migrations have run successfully."));

        AppUser user = new AppUser(email, username, passwordHash);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.addRole(defaultRole);

        AppUser savedUser = userRepository.save(user);
        return toUserDto(savedUser);
    }

    /**
     * Checks if a user exists with the given email address.
     *
     * @param email the email address to check
     * @return true if a user with this email exists
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Checks if a user exists with the given username.
     *
     * @param username the username to check
     * @return true if a user with this username exists
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param id the user's ID
     * @return an Optional containing the UserDto if found
     */
    public Optional<UserDto> getUserById(Long id) {
        return userRepository.findById(id)
            .map(this::toUserDto);
    }

    /**
     * Finds users matching the given search, role, and status filters with pagination.
     *
     * @param search   partial text to match against email, firstName, or lastName (nullable)
     * @param role     role name to filter by, e.g. "ROLE_ADMIN" (nullable)
     * @param status   account status: "active" or "disabled" (nullable)
     * @param pageable pagination and sorting parameters
     * @return a page of matching UserDto instances
     */
    public Page<UserDto> findUsers(String search, String role, String status, Pageable pageable) {
        Specification<AppUser> spec = UserSpecifications.withFilters(search, role, status);
        return userRepository.findAll(spec, pageable).map(this::toUserDto);
    }

    /**
     * Updates the profile fields for the user identified by email.
     * <p>
     * Only non-null, non-blank displayName overwrites the username; firstName and lastName
     * are always updated (may be set to null).
     *
     * @param email       the user's email address (identity)
     * @param displayName new display name (username); ignored if null or blank
     * @param firstName   new first name
     * @param lastName    new last name
     * @return the updated UserDto
     * @throws ResourceNotFoundException if no user exists with the given email
     */
    @Transactional
    public UserDto updateProfile(String email, String displayName, String firstName, String lastName) {
        AppUser user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", email));

        if (displayName != null && !displayName.isBlank()) {
            user.setUsername(displayName);
        }
        user.setFirstName(firstName);
        user.setLastName(lastName);

        AppUser saved = userRepository.save(user);
        return toUserDto(saved);
    }

    /**
     * Updates a user's details by admin. Allows changing name fields and roles.
     *
     * @param id        the user's ID
     * @param firstName new first name
     * @param lastName  new last name
     * @param roleNames the role names to assign (e.g. ["ROLE_USER", "ROLE_ADMIN"]); if null or empty, roles are unchanged
     * @return the updated UserDto
     * @throws ResourceNotFoundException if no user exists with the given ID
     * @throws BadRequestException       if any specified role name does not exist
     */
    @Transactional
    public UserDto updateUser(Long id, String firstName, String lastName, List<String> roleNames) {
        AppUser user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));

        user.setFirstName(firstName);
        user.setLastName(lastName);

        if (roleNames != null && !roleNames.isEmpty()) {
            user.getRoles().clear();
            for (String roleName : roleNames) {
                AppRole role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new BadRequestException("Role not found: " + roleName));
                user.addRole(role);
            }
        }

        AppUser saved = userRepository.save(user);
        return toUserDto(saved);
    }

    /**
     * Enables or disables a user account.
     *
     * @param id      the user's ID
     * @param enabled true to enable, false to disable
     * @return the updated UserDto
     * @throws ResourceNotFoundException if no user exists with the given ID
     */
    @Transactional
    public UserDto toggleUserEnabled(Long id, boolean enabled) {
        AppUser user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));

        user.setEnabled(enabled);

        AppUser saved = userRepository.save(user);
        return toUserDto(saved);
    }

    /**
     * Converts an AppUser entity to a UserDto.
     * <p>
     * This method is private to avoid leaking the entity type.
     *
     * @param user the entity to convert
     * @return the corresponding UserDto
     */
    private UserDto toUserDto(AppUser user) {
        Set<String> roleNames = user.getRoles().stream()
            .map(AppRole::getName)
            .collect(Collectors.toSet());

        return UserDto.from(
            user.getId(),
            user.getEmail(),
            user.getUsername(),
            user.getFirstName(),
            user.getLastName(),
            user.isEnabled(),
            user.isEmailVerified(),
            roleNames,
            user.getCreatedAt()
        );
    }

    /**
     * Converts an AppUser entity to a UserAuthDto.
     * <p>
     * This method is private to avoid leaking the entity type.
     *
     * @param user the entity to convert
     * @return the corresponding UserAuthDto
     */
    private UserAuthDto toUserAuthDto(AppUser user) {
        Set<String> roleNames = user.getRoles().stream()
            .map(AppRole::getName)
            .collect(Collectors.toSet());

        return UserAuthDto.from(
            user.getEmail(),
            user.getPasswordHash(),
            user.isEnabled(),
            user.isEmailVerified(),
            roleNames,
            user.getPasswordChangedAt()
        );
    }
}
