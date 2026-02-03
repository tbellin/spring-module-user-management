package com.example.usermanagement.auth;

import com.example.usermanagement.shared.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JwtService.
 * <p>
 * Tests token generation, validation, and claim extraction using JJWT 0.12.x API.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails testUser;

    // Base64-encoded 256-bit secret (32 bytes = 256 bits for HS256)
    private static final String TEST_SECRET = Base64.getEncoder().encodeToString(
        "this-is-a-test-secret-key-32bytes".getBytes()
    );
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        AppProperties.Jwt jwtProps = new AppProperties.Jwt(TEST_SECRET, EXPIRATION_MS);
        AppProperties appProperties = new AppProperties(jwtProps);
        jwtService = new JwtService(appProperties);

        testUser = User.builder()
            .username("test@example.com")
            .password("hashedpassword")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .build();
    }

    @Test
    @DisplayName("generateToken should create a valid JWT")
    void generateToken_createsValidJwt() {
        String token = jwtService.generateToken(testUser);

        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    @DisplayName("extractUsername should return the subject claim")
    void extractUsername_returnsSubject() {
        String token = jwtService.generateToken(testUser);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("isTokenValid should return true for valid token and matching user")
    void isTokenValid_returnsTrueForValidToken() {
        String token = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(token, testUser);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("isTokenValid should return false for different user")
    void isTokenValid_returnsFalseForDifferentUser() {
        String token = jwtService.generateToken(testUser);

        UserDetails differentUser = User.builder()
            .username("other@example.com")
            .password("password")
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .build();

        boolean isValid = jwtService.isTokenValid(token, differentUser);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Expired token should throw ExpiredJwtException")
    void isTokenValid_throwsForExpiredToken() {
        // Create service with 0ms expiration (immediate expiry)
        AppProperties.Jwt expiredJwtProps = new AppProperties.Jwt(TEST_SECRET, 0);
        AppProperties expiredAppProperties = new AppProperties(expiredJwtProps);
        JwtService expiredJwtService = new JwtService(expiredAppProperties);

        String token = expiredJwtService.generateToken(testUser);

        // Wait a tiny bit to ensure expiration
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // JJWT throws ExpiredJwtException when parsing expired tokens
        assertThatThrownBy(() -> expiredJwtService.isTokenValid(token, testUser))
            .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Invalid token should throw exception on extraction")
    void extractUsername_throwsForInvalidToken() {
        String invalidToken = "invalid.token.here";

        assertThatThrownBy(() -> jwtService.extractUsername(invalidToken))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Token with tampered signature should throw exception")
    void extractUsername_throwsForTamperedToken() {
        String token = jwtService.generateToken(testUser);
        // Tamper with the signature (last part)
        String tamperedToken = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";

        assertThatThrownBy(() -> jwtService.extractUsername(tamperedToken))
            .isInstanceOf(Exception.class);
    }
}
