package com.example.usermanagement.auth.internal.verification;

import com.example.usermanagement.shared.config.AppProperties;
import com.example.usermanagement.user.internal.AppUser;
import com.example.usermanagement.user.internal.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing email verification tokens.
 * <p>
 * Handles token creation, validation, and consumption. Ensures tokens
 * are single-use and old tokens are invalidated when new ones are requested.
 */
@Service
@Transactional(readOnly = true)
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final int expirationHours;
    private final String baseUrl;

    public VerificationService(VerificationTokenRepository tokenRepository,
                               UserRepository userRepository,
                               AppProperties appProperties) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.expirationHours = appProperties.verification().expirationHours();
        this.baseUrl = appProperties.verification().baseUrl();
    }

    /**
     * Creates a new verification token for a user.
     * <p>
     * Invalidates any existing tokens for the user before creating a new one
     * (per CONTEXT.md: "requesting new token invalidates old one").
     *
     * @param user the user to create a token for
     * @return the created token entity
     */
    @Transactional
    public VerificationToken createToken(AppUser user) {
        // Invalidate existing tokens for this user
        tokenRepository.deleteByUser(user);

        String tokenValue = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(expirationHours);

        VerificationToken token = new VerificationToken(tokenValue, user, expiryDate);
        VerificationToken saved = tokenRepository.save(token);

        log.debug("Created verification token for user {}", user.getEmail());
        return saved;
    }

    /**
     * Builds the full verification URL for a token.
     *
     * @param token the verification token
     * @return the complete verification URL
     */
    public String buildVerificationUrl(VerificationToken token) {
        return baseUrl + "/verify/" + token.getToken();
    }

    /**
     * Verifies an email using the provided token.
     * <p>
     * This method atomically marks the token as used to prevent race conditions
     * from double-clicks or concurrent requests.
     *
     * @param tokenValue the token string from the verification link
     * @return the verification result
     */
    @Transactional
    public VerificationResult verifyToken(String tokenValue) {
        Optional<VerificationToken> tokenOpt = tokenRepository.findByToken(tokenValue);

        if (tokenOpt.isEmpty()) {
            log.debug("Verification attempted with invalid token");
            return new VerificationResult.Invalid();
        }

        VerificationToken token = tokenOpt.get();
        AppUser user = token.getUser();

        // Check if user is already verified
        if (user.isEmailVerified()) {
            log.debug("Verification attempted for already verified user: {}", user.getEmail());
            return new VerificationResult.AlreadyVerified();
        }

        // Check if token is expired
        if (token.isExpired()) {
            log.debug("Verification attempted with expired token for user: {}", user.getEmail());
            return new VerificationResult.Expired();
        }

        // Atomically mark token as used (returns 0 if already used)
        int updated = tokenRepository.markAsUsed(tokenValue);
        if (updated == 0) {
            log.debug("Verification attempted with already-used token");
            return new VerificationResult.AlreadyUsed();
        }

        // Activate the user
        user.setEmailVerified(true);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getEmail());
        return new VerificationResult.Success();
    }

    /**
     * Finds a user by email address.
     * <p>
     * Used by resend flow to look up user without exposing UserRepository.
     *
     * @param email the email to search for
     * @return the user if found
     */
    public Optional<AppUser> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
