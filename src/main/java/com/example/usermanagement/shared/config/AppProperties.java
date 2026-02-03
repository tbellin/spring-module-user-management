package com.example.usermanagement.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration properties for the application.
 * <p>
 * Properties are bound from the "app" namespace in application.yml.
 * This class uses the Java record-based approach supported in Spring Boot 3+.
 *
 * @param jwt JWT-related configuration
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt) {

    /**
     * JWT configuration properties.
     *
     * @param secret       Base64-encoded secret key for signing JWTs (min 256 bits for HS256)
     * @param expirationMs Token expiration time in milliseconds
     */
    public record Jwt(String secret, long expirationMs) {
    }
}
