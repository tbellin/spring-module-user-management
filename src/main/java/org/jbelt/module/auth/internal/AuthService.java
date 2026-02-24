package org.jbelt.module.auth.internal;

import org.jbelt.module.auth.JwtService;
import org.jbelt.module.auth.internal.verification.VerificationService;
import org.jbelt.module.auth.internal.verification.VerificationToken;
import org.jbelt.module.shared.dto.UserDto;
import org.jbelt.module.shared.email.EmailService;
import org.jbelt.module.shared.exception.DuplicateResourceException;
import org.jbelt.module.user.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handling user registration and authentication.
 * <p>
 * This service coordinates between UserService for user management,
 * AuthenticationManager for credential validation, and JwtService for
 * token generation. It provides the business logic layer that both
 * REST API and Thymeleaf controllers will use.
 */
@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final VerificationService verificationService;

    public AuthService(UserService userService,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       EmailService emailService,
                       VerificationService verificationService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.verificationService = verificationService;
    }

    /**
     * Registers a new user with the given credentials.
     * <p>
     * The user is created with the default ROLE_USER role.
     * Password is hashed before storage.
     *
     * @param email     the user's email address (also used as username)
     * @param password  the user's plaintext password
     * @param firstName the user's first name
     * @param lastName  the user's last name (optional)
     * @return the created user as a UserDto
     * @throws DuplicateResourceException if email is already registered
     */
    @Transactional
    public UserDto registerUser(String email, String password, String firstName, String lastName) {
        if (userService.existsByEmail(email)) {
            throw new DuplicateResourceException("User", "email");
        }

        String passwordHash = passwordEncoder.encode(password);

        // Use email as username (per RESEARCH.md recommendation)
        UserDto createdUser = userService.createUser(email, email, passwordHash, firstName, lastName);

        // Send verification email
        sendVerificationEmail(email);

        return createdUser;
    }

    /**
     * Creates a verification token and sends verification email.
     * <p>
     * Package-private for use by resend flow in AuthController.
     *
     * @param email the user's email address
     */
    void sendVerificationEmail(String email) {
        verificationService.findUserByEmail(email).ifPresent(user -> {
            VerificationToken token = verificationService.createToken(user);
            String verificationUrl = verificationService.buildVerificationUrl(token);
            emailService.sendVerificationEmail(email, verificationUrl);
        });
    }

    /**
     * Authenticates a user with the given credentials.
     * <p>
     * Delegates to Spring Security's AuthenticationManager for credential validation.
     * AuthenticationException is allowed to propagate for handling by Spring Security.
     *
     * @param email    the user's email address
     * @param password the user's plaintext password
     * @return the Authentication object if successful
     */
    public Authentication authenticate(String email, String password) {
        UsernamePasswordAuthenticationToken token =
            new UsernamePasswordAuthenticationToken(email, password);
        return authenticationManager.authenticate(token);
    }

    /**
     * Generates a JWT token for the authenticated user.
     *
     * @param authentication the successful authentication result
     * @param rememberMe     whether to use extended expiration
     * @return the signed JWT token string
     */
    public String generateToken(Authentication authentication, boolean rememberMe) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return jwtService.generateToken(userDetails, rememberMe);
    }

    /**
     * Returns the token expiration time in seconds.
     * <p>
     * Used for populating the expiresIn field in AuthResponse.
     *
     * @param rememberMe whether remember-me expiration is used
     * @return expiration time in seconds
     */
    public long getTokenExpiration(boolean rememberMe) {
        long expirationMs = rememberMe
            ? jwtService.getRememberMeExpirationMs()
            : jwtService.getExpirationMs();
        return expirationMs / 1000;
    }
}
