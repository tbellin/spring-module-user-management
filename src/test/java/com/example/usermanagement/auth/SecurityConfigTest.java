package com.example.usermanagement.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for SecurityConfig.
 * <p>
 * Verifies SUCCESS CRITERIA #1 (dual filter chains) and #3 (401/403 responses).
 * Tests both API endpoints (stateless, JWT) and web endpoints (session, CSRF).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("API Filter Chain (/api/**)")
    class ApiFilterChainTests {

        @Test
        @DisplayName("Unauthenticated request to protected API returns 401 with JSON ProblemDetail")
        void unauthenticatedApiRequest_returns401() throws Exception {
            mockMvc.perform(get("/api/users/me")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("Public auth endpoints are accessible without authentication")
        void publicAuthEndpoints_areAccessible() throws Exception {
            // /api/auth/** is permitAll - will return 404 since endpoint doesn't exist yet
            // but NOT 401, proving the path is public
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"test@example.com\",\"password\":\"test\"}"))
                .andExpect(status().isNotFound()); // 404, not 401
        }

        @Test
        @DisplayName("USER role accessing admin API returns 403")
        void userAccessingAdminApi_returns403() throws Exception {
            // Use SecurityMockMvcRequestPostProcessors for proper filter chain integration
            mockMvc.perform(get("/api/admin/users")
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user@example.com").roles("USER"))
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("ADMIN role accessing admin API succeeds (or returns 404 if endpoint doesn't exist)")
        void adminAccessingAdminApi_succeeds() throws Exception {
            // Endpoint doesn't exist yet, so expect 404 (not 403)
            mockMvc.perform(get("/api/admin/users")
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin@example.com").roles("ADMIN"))
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // 404, not 403 - authorization passed
        }

        @Test
        @DisplayName("Authenticated USER can access protected non-admin API")
        void authenticatedUser_canAccessProtectedApi() throws Exception {
            // Endpoint doesn't exist yet, so expect 404 (not 401/403)
            mockMvc.perform(get("/api/users/me")
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user@example.com").roles("USER"))
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // 404, not 401 - authentication passed
        }
    }

    @Nested
    @DisplayName("Web Filter Chain (/**)")
    class WebFilterChainTests {

        @Test
        @DisplayName("Public pages are accessible without authentication")
        void publicPages_areAccessible() throws Exception {
            mockMvc.perform(get("/"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Login URL is public (returns 404 until controller exists)")
        void loginUrl_isPublic() throws Exception {
            // /login is permitAll but no controller exists yet
            // Key verification: returns 404 (not 401/302) proving the path is public
            mockMvc.perform(get("/login"))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Unauthenticated request to protected page redirects to login")
        void unauthenticatedWebRequest_redirectsToLogin() throws Exception {
            mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        }

        @Test
        @DisplayName("USER role accessing admin page returns 403")
        void userAccessingAdminPage_returns403() throws Exception {
            mockMvc.perform(get("/admin/users")
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ADMIN role can access admin pages")
        void adminAccessingAdminPage_succeeds() throws Exception {
            // Page doesn't exist yet, but should NOT be 403
            mockMvc.perform(get("/admin/users")
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNotFound()); // 404, not 403
        }

        @Test
        @DisplayName("Actuator health endpoint is public")
        void actuatorHealth_isPublic() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("CSRF Protection")
    class CsrfTests {

        @Test
        @DisplayName("API POST without CSRF token succeeds (CSRF disabled for API)")
        void apiPostWithoutCsrf_succeeds() throws Exception {
            // /api/auth/login is public and API chain has CSRF disabled
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"test@example.com\",\"password\":\"test\"}"))
                .andExpect(status().isNotFound()); // 404 (endpoint not implemented), not 403 (CSRF)
        }
    }
}
