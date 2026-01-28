package com.example.usermanagement.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Minimal security configuration for Phase 1 bootstrap.
 * This will be replaced with dual SecurityFilterChain in Phase 2.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public paths for Phase 1 smoke testing
                .requestMatchers("/", "/error").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // Static resources
                .requestMatchers("/css/**", "/js/**", "/webjars/**").permitAll()
                // Everything else requires authentication (placeholder for later)
                .anyRequest().authenticated()
            )
            // H2 console uses frames - allow same-origin frames
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )
            // Disable CSRF for H2 console (uses POST for queries)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
            );

        return http.build();
    }
}
