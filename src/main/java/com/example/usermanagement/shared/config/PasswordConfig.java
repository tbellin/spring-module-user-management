package com.example.usermanagement.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password encoding configuration.
 * <p>
 * The PasswordEncoder is defined in shared.config (not auth.internal) because
 * it's a cross-cutting infrastructure concern. Both the auth module (for login)
 * and the user module (for admin-created users) may need to encode passwords.
 * <p>
 * SUCCESS CRITERIA #2: Passwords are stored using BCrypt hashing.
 */
@Configuration
public class PasswordConfig {

    /**
     * BCrypt password encoder with default strength (10).
     * <p>
     * BCrypt includes salt automatically and is intentionally slow to
     * resist brute-force attacks. Strength 10 provides a good balance
     * between security and performance.
     *
     * @return the password encoder bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
