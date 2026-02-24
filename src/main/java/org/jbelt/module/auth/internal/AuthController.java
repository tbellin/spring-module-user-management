package org.jbelt.module.auth.internal;

import org.jbelt.module.auth.AuthResponse;
import org.jbelt.module.auth.internal.verification.ResendRateLimiter;
import org.jbelt.module.shared.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller for authentication endpoints.
 * <p>
 * Provides stateless JWT authentication for API clients (mobile apps, SPAs, cURL testing).
 * <p>
 * Endpoints:
 * <ul>
 *   <li>POST /api/v1/auth/register - User registration</li>
 *   <li>POST /api/v1/auth/login - User authentication</li>
 *   <li>POST /api/v1/auth/resend-verification - Resend verification email</li>
 * </ul>
 * <p>
 * Error handling is delegated to GlobalExceptionHandler:
 * <ul>
 *   <li>DuplicateResourceException (409) - email already registered</li>
 *   <li>AuthenticationException (401) - invalid credentials</li>
 *   <li>MethodArgumentNotValidException (400) - validation errors</li>
 * </ul>
 */
@Tag(name = "Authentication", description = "User registration, login, and email verification")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final ResendRateLimiter rateLimiter;

    public AuthController(AuthService authService, ResendRateLimiter rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Registers a new user and returns confirmation to check email.
     * <p>
     * Creates the user and sends a verification email. The user must verify
     * their email before they can log in. No JWT is returned at registration
     * since the account is not yet verified.
     *
     * @param request the registration details (email, password, firstName, lastName)
     * @return 201 Created with confirmation message
     */
    @Operation(summary = "Register new user",
              description = "Creates a user account and sends a verification email. The user must verify their email before logging in.")
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegistrationRequest request) {
        // Create the user (sends verification email)
        UserDto user = authService.registerUser(
            request.email(),
            request.password(),
            request.firstName(),
            request.lastName()
        );

        // Return confirmation (no JWT - user must verify email first)
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Registration successful. Please check your email to verify your account.");
        response.put("email", user.email());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates a user and returns a JWT token.
     * <p>
     * Validates credentials and generates a token with optional extended expiration
     * when rememberMe is true.
     *
     * @param request the login credentials (email, password, rememberMe)
     * @return 200 OK with AuthResponse containing JWT
     */
    @Operation(summary = "Authenticate user",
              description = "Validates credentials and returns a JWT token. Use the token in the Authorize dialog to access protected endpoints.")
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentication successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // Authenticate user
        Authentication authentication = authService.authenticate(
            request.email(),
            request.password()
        );

        // Generate token with remember-me support
        String token = authService.generateToken(authentication, request.rememberMe());
        long expiresIn = authService.getTokenExpiration(request.rememberMe());

        // Extract user info from authentication
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Set<String> roles = extractRoles(authentication);

        // Build response
        AuthResponse response = new AuthResponse(
            token,
            expiresIn,
            userDetails.getUsername(), // email used as username
            userDetails.getUsername(), // displayName from UserDetails (will be enhanced when UserDetails has more info)
            roles
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Resends verification email.
     * <p>
     * SEC-01 compliance: Returns the same message regardless of whether
     * the account exists, is already verified, or doesn't exist.
     * This prevents user enumeration attacks.
     * <p>
     * Rate limited: 60 second cooldown between resend attempts per email.
     *
     * @param request the resend request containing email
     * @return success message (always the same for SEC-01)
     */
    @Operation(summary = "Resend verification email",
              description = "Sends a new verification email. Returns the same response regardless of whether the email exists (SEC-01).")
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Request processed"),
        @ApiResponse(responseCode = "429", description = "Rate limited")
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {

        String email = request.email();

        // Check rate limit
        if (!rateLimiter.canResend(email)) {
            long remaining = rateLimiter.getRemainingCooldownSeconds(email);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                    "message", "Please wait before requesting another verification email",
                    "retryAfter", String.valueOf(remaining)
                ));
        }

        // Send email (if account exists and is unverified)
        // SEC-01: Same response regardless of outcome
        authService.sendVerificationEmail(email);
        rateLimiter.recordResend(email);

        return ResponseEntity.ok(Map.of(
            "message", "If an account exists with this email, a verification link has been sent"
        ));
    }

    /**
     * Extracts role names from the authentication's granted authorities.
     */
    private Set<String> extractRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
    }
}
