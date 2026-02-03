package com.example.usermanagement.auth;

import com.example.usermanagement.shared.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * Service for JWT token generation and validation.
 * <p>
 * This is the public API of the auth module for JWT operations.
 * Uses JJWT 0.12.x API with HS256 signing algorithm.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(AppProperties appProperties) {
        // Decode Base64-encoded secret to create signing key
        // The secret must be at least 256 bits (32 bytes) for HS256
        this.signingKey = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(appProperties.jwt().secret())
        );
        this.expirationMs = appProperties.jwt().expirationMs();
    }

    /**
     * Generates a JWT token for the given user details.
     *
     * @param userDetails the authenticated user
     * @return a signed JWT token string
     */
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
            .subject(userDetails.getUsername())
            .claim("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(signingKey)
            .compact();
    }

    /**
     * Extracts the username (subject) from the token.
     *
     * @param token the JWT token
     * @return the username
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Validates the token against the user details.
     *
     * @param token       the JWT token
     * @param userDetails the user details to validate against
     * @return true if the token is valid for this user and not expired
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Extracts a specific claim from the token.
     *
     * @param token    the JWT token
     * @param resolver function to extract the desired claim
     * @param <T>      the claim type
     * @return the extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return resolver.apply(claims);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}
