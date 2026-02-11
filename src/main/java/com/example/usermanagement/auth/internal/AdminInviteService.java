package com.example.usermanagement.auth.internal;

import com.example.usermanagement.auth.internal.password.PasswordResetToken;
import com.example.usermanagement.auth.internal.password.PasswordResetTokenRepository;
import com.example.usermanagement.shared.config.AppProperties;
import com.example.usermanagement.shared.dto.UserDto;
import com.example.usermanagement.shared.email.EmailService;
import com.example.usermanagement.shared.exception.BadRequestException;
import com.example.usermanagement.shared.exception.DuplicateResourceException;
import com.example.usermanagement.user.UserService;
import com.example.usermanagement.user.internal.AppRole;
import com.example.usermanagement.user.internal.AppUser;
import com.example.usermanagement.user.internal.RoleRepository;
import com.example.usermanagement.user.internal.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for admin-initiated user invitations.
 * <p>
 * Coordinates user creation with invite email sending, reusing the existing
 * {@link PasswordResetToken} infrastructure for the set-password link.
 * <p>
 * Placed in the {@code auth.internal} package because it needs access to
 * {@link PasswordResetTokenRepository} (auth module) as well as
 * {@link UserRepository} and {@link RoleRepository} (user module).
 * The auth module's allowed dependencies include both "user" and "shared".
 */
@Service
@Transactional(readOnly = true)
public class AdminInviteService {

    private static final Logger log = LoggerFactory.getLogger(AdminInviteService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final int expirationHours;
    private final String baseUrl;

    public AdminInviteService(UserRepository userRepository,
                              RoleRepository roleRepository,
                              PasswordResetTokenRepository tokenRepository,
                              EmailService emailService,
                              UserService userService,
                              PasswordEncoder passwordEncoder,
                              AppProperties appProperties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.expirationHours = appProperties.verification().expirationHours();
        this.baseUrl = appProperties.verification().baseUrl();
    }

    /**
     * Invites a new user by creating their account and sending an invite email.
     * <p>
     * The user is created with:
     * <ul>
     *     <li>{@code emailVerified = true} (admin-verified, no email confirmation needed)</li>
     *     <li>{@code enabled = true}</li>
     *     <li>A placeholder password (random BCrypt hash that cannot be guessed)</li>
     * </ul>
     * A {@link PasswordResetToken} is generated and an invite email is sent with
     * a set-password link pointing to {@code /reset-password?token=...}.
     *
     * @param email    the email address of the user to invite
     * @param roleName the role to assign (e.g. "ROLE_USER", "ROLE_ADMIN")
     * @return the created user as a {@link UserDto}
     * @throws DuplicateResourceException if a user with the given email already exists
     * @throws BadRequestException        if the specified role does not exist
     */
    @Transactional
    public UserDto inviteUser(String email, String roleName) {
        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User", "email");
        }

        // Find the requested role
        AppRole role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new BadRequestException("Invalid role: " + roleName));

        // Create user with placeholder password (valid BCrypt hash that no one knows)
        String placeholder = passwordEncoder.encode(UUID.randomUUID().toString());
        AppUser user = new AppUser(email, email, placeholder);
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.addRole(role);
        AppUser saved = userRepository.save(user);

        // Generate password reset token (reuse existing infrastructure)
        String tokenValue = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(expirationHours);
        PasswordResetToken token = new PasswordResetToken(tokenValue, saved, expiryDate);
        tokenRepository.save(token);

        // Send invite email with set-password link
        String setPasswordUrl = baseUrl + "/reset-password?token=" + tokenValue;
        emailService.sendInviteEmail(email, setPasswordUrl);

        log.info("User invited by admin: {}", email);

        // Reuse UserService for DTO conversion to avoid duplicating logic
        return userService.getUserByEmail(email)
            .orElseThrow(() -> new IllegalStateException("User was just created but not found: " + email));
    }
}
