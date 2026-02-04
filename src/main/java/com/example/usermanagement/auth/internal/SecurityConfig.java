package com.example.usermanagement.auth.internal;

import com.example.usermanagement.auth.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration with dual SecurityFilterChain.
 * <p>
 * SUCCESS CRITERIA #1: Two filter chains - API (stateless, JWT) and Web (session, CSRF).
 * SUCCESS CRITERIA #3: Protected endpoints return 401/403 appropriately.
 * <p>
 * The API chain (@Order 1) matches /api/** and is stateless with JWT authentication.
 * The web chain (@Order 2) matches everything else and uses sessions with CSRF and form login.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * API SecurityFilterChain - stateless, JWT-based, no CSRF.
     * <p>
     * Matches: /api/**
     * Order: 1 (higher priority than web chain)
     * Session: STATELESS
     * CSRF: Disabled (stateless APIs don't need CSRF protection)
     * Authentication: Bearer JWT via JwtAuthenticationFilter
     * Error responses: JSON ProblemDetail (401/403)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        // Create filter instance (NOT a bean to avoid global registration)
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new ApiAuthenticationEntryPoint())
                .accessDeniedHandler(new ApiAccessDeniedHandler()))
            .authorizeHttpRequests(auth -> auth
                // Public API endpoints
                .requestMatchers("/api/auth/**").permitAll()      // Keep for backward compat
                .requestMatchers("/api/v1/auth/**").permitAll()   // Versioned auth endpoints
                // Admin-only endpoints (SUCCESS CRITERIA #3)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")  // Future admin API
                // All other API endpoints require authentication
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Web SecurityFilterChain - session-based, CSRF enabled, form login.
     * <p>
     * Matches: /** (everything not matched by API chain)
     * Order: 2 (lower priority than API chain)
     * Session: Default (session-based)
     * CSRF: Enabled (required for form submissions, SUCCESS CRITERIA #1)
     * Authentication: Form login with Thymeleaf pages
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public pages
                .requestMatchers("/", "/error").permitAll()
                .requestMatchers("/login", "/register").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Static resources
                .requestMatchers("/css/**", "/js/**", "/webjars/**").permitAll()
                // H2 console (dev only)
                .requestMatchers("/h2-console/**").permitAll()
                // Admin pages
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // All other pages require authentication
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/")
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll())
            .rememberMe(remember -> remember
                .key("user-management-remember-me-key")
                .tokenValiditySeconds(7 * 24 * 60 * 60)  // 7 days
                .rememberMeParameter("remember-me")
                .userDetailsService(userDetailsService))
            // H2 console uses frames
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin()))
            // CSRF enabled by default, but disable for H2 console
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**"));

        return http.build();
    }

    /**
     * AuthenticationManager bean for use in auth controllers.
     * <p>
     * Required for programmatic authentication in login endpoints.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
