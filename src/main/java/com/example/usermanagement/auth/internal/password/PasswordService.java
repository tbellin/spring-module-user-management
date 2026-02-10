package com.example.usermanagement.auth.internal.password;

import com.example.usermanagement.shared.config.AppProperties;
import com.example.usermanagement.shared.email.EmailService;
import com.example.usermanagement.shared.exception.BadRequestException;
import com.example.usermanagement.shared.exception.ResourceNotFoundException;
import com.example.usermanagement.user.internal.AppUser;
import com.example.usermanagement.user.internal.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Core business logic for password management.
 * <p>
 * Provides three operations:
 * <ul>
 *     <li>{@link #changePassword} - Authenticated user changes password (validates current password)</li>
 *     <li>{@link #requestPasswordReset} - Generates reset token and sends email (SEC-01 compliant)</li>
 *     <li>{@link #resetPassword} - Validates token and sets new password</li>
 * </ul>
 * <p>
 * Both {@code changePassword} and {@code resetPassword} set {@code passwordChangedAt}
 * on the user, which causes all previously-issued JWT tokens to be rejected by
 * {@link com.example.usermanagement.auth.internal.JwtAuthenticationFilter}.
 * <p>
 * Rate limiting is handled by the calling controllers (PasswordController and
 * PasswordWebController), not by this service.
 */
@Service
@Transactional(readOnly = true)
public class PasswordService {

    private static final Logger log = LoggerFactory.getLogger(PasswordService.class);

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final int expirationHours;
    private final String baseUrl;

    public PasswordService(PasswordResetTokenRepository tokenRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           EmailService emailService,
                           AppProperties appProperties) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.expirationHours = appProperties.verification().expirationHours();
        this.baseUrl = appProperties.verification().baseUrl();
    }

    /**
     * Changes the password for an authenticated user.
     * <p>
     * Validates the current password before setting the new one.
     * Sets {@code passwordChangedAt} to invalidate all existing JWT tokens.
     *
     * @param email           the user's email address
     * @param currentPassword the user's current password (for verification)
     * @param newPassword     the new password (plain text, will be encoded)
     * @throws ResourceNotFoundException if user not found
     * @throws BadRequestException       if current password is incorrect
     */
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        AppUser user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", email));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password changed for user: {}", email);
    }

    /**
     * Requests a password reset for the given email address.
     * <p>
     * SEC-01 compliance: This method silently does nothing if the email is not
     * found or is not verified. The caller always shows the same response
     * regardless of whether the email exists.
     * <p>
     * Rate limiting is handled by the calling controller, not here.
     *
     * @param email the email address to send the reset link to
     */
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            // Only proceed if email is verified -- don't help unverified accounts
            if (user.isEmailVerified()) {
                // Delete existing reset tokens for this user
                tokenRepository.deleteByUser(user);

                // Create new token
                String tokenValue = UUID.randomUUID().toString();
                LocalDateTime expiryDate = LocalDateTime.now().plusHours(expirationHours);
                PasswordResetToken token = new PasswordResetToken(tokenValue, user, expiryDate);
                tokenRepository.save(token);

                // Build reset URL and send email
                String resetUrl = baseUrl + "/reset-password?token=" + tokenValue;
                emailService.sendPasswordResetEmail(email, resetUrl);

                log.info("Password reset email requested for user: {}", email);
            }
        });
    }

    /**
     * Resets a user's password using a reset token.
     * <p>
     * Validates the token, atomically marks it as used, sets the new password,
     * and updates {@code passwordChangedAt} to invalidate all existing JWT tokens.
     * Cleans up all reset tokens for the user after successful reset.
     *
     * @param tokenValue  the reset token from the email link
     * @param newPassword the new password (plain text, will be encoded)
     * @return the result of the reset operation
     */
    @Transactional
    public PasswordResetResult resetPassword(String tokenValue, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(tokenValue);

        if (tokenOpt.isEmpty()) {
            log.debug("Password reset attempted with invalid token");
            return new PasswordResetResult.Invalid();
        }

        PasswordResetToken token = tokenOpt.get();

        if (token.isExpired()) {
            log.debug("Password reset attempted with expired token for user: {}", token.getUser().getEmail());
            return new PasswordResetResult.Expired();
        }

        // Atomically mark token as used (returns 0 if already used)
        int updated = tokenRepository.markAsUsed(tokenValue);
        if (updated == 0) {
            log.debug("Password reset attempted with already-used token");
            return new PasswordResetResult.AlreadyUsed();
        }

        // Set new password and update timestamp for JWT invalidation
        AppUser user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        // Cleanup: delete all reset tokens for this user
        tokenRepository.deleteByUser(user);

        log.info("Password successfully reset for user: {}", user.getEmail());
        return new PasswordResetResult.Success();
    }
}
