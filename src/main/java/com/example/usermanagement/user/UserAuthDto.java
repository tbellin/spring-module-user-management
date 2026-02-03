package com.example.usermanagement.user;

import java.util.Set;

/**
 * Data transfer object for user authentication details.
 * <p>
 * This record contains the information needed by the auth module to
 * authenticate users and check their authorization. It includes the
 * password hash (which should not be exposed via the general UserDto).
 *
 * @param email         the user's email address (used as login identifier)
 * @param passwordHash  the user's hashed password
 * @param enabled       whether the user account is enabled
 * @param emailVerified whether the user's email has been verified
 * @param roles         the user's role names (e.g., "ROLE_USER", "ROLE_ADMIN")
 */
public record UserAuthDto(
    String email,
    String passwordHash,
    boolean enabled,
    boolean emailVerified,
    Set<String> roles
) {

    /**
     * Creates a UserAuthDto from individual field values.
     *
     * @param email         the user's email address
     * @param passwordHash  the user's hashed password
     * @param enabled       whether the user account is enabled
     * @param emailVerified whether the user's email has been verified
     * @param roles         the user's role names
     * @return a new UserAuthDto instance
     */
    public static UserAuthDto from(
        String email,
        String passwordHash,
        boolean enabled,
        boolean emailVerified,
        Set<String> roles
    ) {
        return new UserAuthDto(email, passwordHash, enabled, emailVerified, roles);
    }
}
