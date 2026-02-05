package com.example.usermanagement.auth.internal;

import com.example.usermanagement.user.UserAuthDto;
import com.example.usermanagement.user.UserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom UserDetailsService implementation that bridges Spring Security with the user module.
 * <p>
 * This service is internal to the auth module. It calls the user module's public
 * UserService API to load user authentication details.
 * <p>
 * SECURITY NOTE (SEC-01): Error messages do NOT reveal whether an email exists.
 * All auth failures return the generic "Bad credentials" message.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public CustomUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    /**
     * Loads user details by username (email in this system).
     * <p>
     * IMPORTANT: The exception message is generic ("Bad credentials") to prevent
     * user enumeration attacks (SEC-01). Do NOT use messages like "User not found"
     * or "Email does not exist". The same message is used for:
     * <ul>
     *     <li>Non-existent accounts</li>
     *     <li>Unverified accounts</li>
     *     <li>Wrong password (handled by Spring Security later in the auth flow)</li>
     * </ul>
     *
     * @param username the email address (used as username)
     * @return UserDetails for Spring Security authentication
     * @throws UsernameNotFoundException if user not found or email not verified (with generic message)
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAuthDto authDto = userService.getUserAuthDetailsByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));

        // SEC-01: Block unverified users with SAME error message as non-existent users
        // This prevents user enumeration - attacker cannot distinguish between
        // "account doesn't exist" and "account exists but unverified"
        if (!authDto.emailVerified()) {
            throw new UsernameNotFoundException("Bad credentials");
        }

        // Convert roles to Spring Security authorities
        var authorities = authDto.roles().stream()
            .map(SimpleGrantedAuthority::new)
            .toList();

        // Use Spring Security's built-in User builder
        // Do NOT have AppUser implement UserDetails (couples domain to security)
        return User.builder()
            .username(authDto.email())
            .password(authDto.passwordHash())
            .authorities(authorities)
            .disabled(!authDto.enabled())
            .accountLocked(false)  // Not implemented yet
            .accountExpired(false) // Not implemented yet
            .credentialsExpired(false) // Not implemented yet
            .build();
    }
}
