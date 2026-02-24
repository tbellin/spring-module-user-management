package org.jbelt.module.auth.internal;

import org.jbelt.module.auth.internal.password.PasswordResetToken;
import org.jbelt.module.auth.internal.password.PasswordResetTokenRepository;
import org.jbelt.module.shared.config.AppProperties;
import org.jbelt.module.shared.dto.UserDto;
import org.jbelt.module.shared.email.EmailService;
import org.jbelt.module.shared.exception.BadRequestException;
import org.jbelt.module.shared.exception.DuplicateResourceException;
import org.jbelt.module.user.UserService;
import org.jbelt.module.user.internal.AppRole;
import org.jbelt.module.user.internal.AppUser;
import org.jbelt.module.user.internal.RoleRepository;
import org.jbelt.module.user.internal.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
     * Result of an admin invite operation.
     *
     * @param user           the created user
     * @param emailSent      whether the invite email was sent successfully
     * @param setPasswordUrl the URL for the user to set their password
     */
    public record InviteResult(UserDto user, boolean emailSent, String setPasswordUrl) {}

    /**
     * Invites a new user by creating their account and sending an invite email.
     * <p>
     * The user is created with:
     * <ul>
     *     <li>{@code emailVerified = true} (admin-verified, no email confirmation needed)</li>
     *     <li>A placeholder password (random BCrypt hash that cannot be guessed)</li>
     * </ul>
     * A {@link PasswordResetToken} is generated and an invite email is sent with
     * a set-password link pointing to {@code /reset-password?token=...}.
     * <p>
     * If email sending fails, the user is still created and the set-password URL
     * is returned so the admin can share it manually.
     *
     * @param email     the email address of the user to invite
     * @param username  the display name / username (nullable, defaults to email)
     * @param firstName the user's first name (nullable)
     * @param lastName  the user's last name (nullable)
     * @param enabled   whether the account should be enabled immediately
     * @param roleNames the roles to assign (e.g. ["ROLE_USER", "ROLE_ADMIN"])
     * @return an {@link InviteResult} with user, email status, and set-password URL
     * @throws DuplicateResourceException if a user with the given email already exists
     * @throws BadRequestException        if any specified role does not exist
     */
    @Transactional
    public InviteResult inviteUser(String email, String username, String firstName,
                                   String lastName, boolean enabled, List<String> roleNames) {
        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User", "email");
        }

        // Find the requested roles
        if (roleNames == null || roleNames.isEmpty()) {
            throw new BadRequestException("At least one role must be specified");
        }

        // Create user with placeholder password (valid BCrypt hash that no one knows)
        String placeholder = passwordEncoder.encode(UUID.randomUUID().toString());
        String displayName = (username != null && !username.isBlank()) ? username : email;
        AppUser user = new AppUser(email, displayName, placeholder);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmailVerified(true);
        user.setEnabled(enabled);

        for (String roleName : roleNames) {
            AppRole role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BadRequestException("Invalid role: " + roleName));
            user.addRole(role);
        }

        AppUser saved = userRepository.save(user);

        // Generate password reset token (reuse existing infrastructure)
        String tokenValue = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(expirationHours);
        PasswordResetToken token = new PasswordResetToken(tokenValue, saved, expiryDate);
        tokenRepository.save(token);

        // Send invite email with set-password link (best-effort)
        String setPasswordUrl = baseUrl + "/reset-password?token=" + tokenValue;
        boolean emailSent = false;
        try {
            emailService.sendInviteEmail(email, setPasswordUrl);
            emailSent = true;
            log.info("User invited by admin: {}", email);
        } catch (Exception e) {
            log.warn("User created but invite email failed for {}: {}", email, e.getMessage());
        }

        // Reuse UserService for DTO conversion to avoid duplicating logic
        UserDto userDto = userService.getUserByEmail(email)
            .orElseThrow(() -> new IllegalStateException("User was just created but not found: " + email));

        return new InviteResult(userDto, emailSent, setPasswordUrl);
    }
}
