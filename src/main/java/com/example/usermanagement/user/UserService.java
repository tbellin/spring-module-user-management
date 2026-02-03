package com.example.usermanagement.user;

import com.example.usermanagement.shared.dto.UserDto;
import com.example.usermanagement.user.internal.AppRole;
import com.example.usermanagement.user.internal.AppUser;
import com.example.usermanagement.user.internal.RoleRepository;
import com.example.usermanagement.user.internal.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            roleNames
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
            roleNames
        );
    }
}
