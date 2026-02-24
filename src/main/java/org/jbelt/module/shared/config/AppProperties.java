package org.jbelt.module.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration properties for the application.
 * <p>
 * Properties are bound from the "app" namespace in application.yml.
 * This class uses the Java record-based approach supported in Spring Boot 3+.
 *
 * @param jwt          JWT-related configuration
 * @param mail         Mail sender configuration
 * @param verification Email verification configuration
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Mail mail, Verification verification) {

    /**
     * JWT configuration properties.
     *
     * @param secret                 Base64-encoded secret key for signing JWTs (min 256 bits for HS256)
     * @param expirationMs           Token expiration time in milliseconds
     * @param rememberMeExpirationMs Token expiration time for remember-me sessions (milliseconds)
     */
    public record Jwt(String secret, long expirationMs, long rememberMeExpirationMs) {
    }

    /**
     * Mail sender configuration properties.
     *
     * @param from Sender email address for outgoing emails
     */
    public record Mail(String from) {
    }

    /**
     * Email verification configuration properties.
     *
     * @param expirationHours Hours before a verification token expires
     * @param baseUrl         Base URL for constructing verification links
     */
    public record Verification(int expirationHours, String baseUrl) {
    }
}
