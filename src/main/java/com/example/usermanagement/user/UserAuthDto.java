package com.example.usermanagement.user;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Data transfer object for user authentication details.
 * <p>
 * This record contains the information needed by the auth module to
 * authenticate users and check their authorization. It includes the
 * password hash (which should not be exposed via the general UserDto).
 *
 * @param email              the user's email address (used as login identifier)
 * @param passwordHash       the user's hashed password
 * @param enabled            whether the user account is enabled
 * @param emailVerified      whether the user's email has been verified
 * @param roles              the user's role names (e.g., "ROLE_USER", "ROLE_ADMIN")
 * @param passwordChangedAt  when the user's password was last changed (null if never changed)
 */
public record UserAuthDto(
    String email,
    String passwordHash,
    boolean enabled,
    boolean emailVerified,
    Set<String> roles,
    LocalDateTime passwordChangedAt
) {

    /**
     * Creates a UserAuthDto from individual field values.
     *
     * @param email              the user's email address
     * @param passwordHash       the user's hashed password
     * @param enabled            whether the user account is enabled
     * @param emailVerified      whether the user's email has been verified
     * @param roles              the user's role names
     * @param passwordChangedAt  when the password was last changed (may be null)
     * @return a new UserAuthDto instance
     */
    public static UserAuthDto from(
        String email,
        String passwordHash,
        boolean enabled,
        boolean emailVerified,
        Set<String> roles,
        LocalDateTime passwordChangedAt
    ) {
        return new UserAuthDto(email, passwordHash, enabled, emailVerified, roles, passwordChangedAt);
    }
}
