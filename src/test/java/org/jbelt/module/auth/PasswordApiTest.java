package org.jbelt.module.auth;

import org.jbelt.module.shared.email.EmailService;
import org.jbelt.module.user.internal.AppUser;
import org.jbelt.module.user.internal.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for REST API password management endpoints.
 * <p>
 * Tests cover:
 * - POST /api/v1/auth/change-password (authenticated)
 * - POST /api/v1/auth/forgot-password (public, SEC-01 compliant)
 * - POST /api/v1/auth/reset-password (public, token-based)
 * <p>
 * EmailService is mocked to prevent actual SMTP calls while keeping
 * the JavaMailSender bean intact for actuator health checks.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PasswordApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailService emailService;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    // ========== Change Password Tests ==========

    @Nested
    @DisplayName("POST /api/v1/auth/change-password")
    class ChangePasswordTests {

        @Test
        @DisplayName("With valid credentials returns 200")
        void changePassword_withValidCredentials_returns200() throws Exception {
            String email = "chgpwd-" + UUID.randomUUID() + "@example.com";
            String currentPassword = "password123";
            registerAndVerifyUser(email, currentPassword, "Change Pwd User");

            var request = Map.of(
                "currentPassword", currentPassword,
                "newPassword", "newpassword456",
                "confirmPassword", "newpassword456"
            );

            mockMvc.perform(post("/api/v1/auth/change-password")
                    .with(user(email).roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
        }

        @Test
        @DisplayName("Without authentication returns 401")
        void changePassword_withoutAuth_returns401() throws Exception {
            var request = Map.of(
                "currentPassword", "password123",
                "newPassword", "newpassword456",
                "confirmPassword", "newpassword456"
            );

            mockMvc.perform(post("/api/v1/auth/change-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
        }

        @Test
        @DisplayName("Password mismatch returns 400")
        void changePassword_passwordMismatch_returns400() throws Exception {
            String email = "chgmis-" + UUID.randomUUID() + "@example.com";
            registerAndVerifyUser(email, "password123", "Mismatch User");

            var request = Map.of(
                "currentPassword", "password123",
                "newPassword", "newpassword456",
                "confirmPassword", "differentpassword789"
            );

            mockMvc.perform(post("/api/v1/auth/change-password")
                    .with(user(email).roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password and confirmation do not match"));
        }
    }

    // ========== Forgot Password Tests ==========

    @Nested
    @DisplayName("POST /api/v1/auth/forgot-password")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Existing email returns 200 with generic message")
        void forgotPassword_existingEmail_returns200() throws Exception {
            String email = "forgot-" + UUID.randomUUID() + "@example.com";
            registerAndVerifyUser(email, "password123", "Forgot Pwd User");

            var request = Map.of("email", email);

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                    "If an account exists with this email, a password reset link has been sent"));
        }

        @Test
        @DisplayName("Non-existing email returns 200 with same message (SEC-01)")
        void forgotPassword_nonExistingEmail_returns200() throws Exception {
            String email = "nonexist-" + UUID.randomUUID() + "@example.com";

            var request = Map.of("email", email);

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                    "If an account exists with this email, a password reset link has been sent"));
        }

        @Test
        @DisplayName("Rapid duplicate requests return 429")
        void forgotPassword_rateLimited_returns429() throws Exception {
            String email = "ratelimit-" + UUID.randomUUID() + "@example.com";
            registerAndVerifyUser(email, "password123", "Rate Limit User");

            var request = Map.of("email", email);
            String json = jsonMapper.writeValueAsString(request);

            // First request should succeed
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                .andExpect(status().isOk());

            // Second request immediately should be rate limited
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.retryAfter").exists());
        }
    }

    // ========== Reset Password Tests ==========

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password")
    class ResetPasswordTests {

        @Test
        @DisplayName("Invalid token returns 400")
        void resetPassword_invalidToken_returns400() throws Exception {
            var request = Map.of(
                "token", UUID.randomUUID().toString(),
                "newPassword", "newpassword456",
                "confirmPassword", "newpassword456"
            );

            mockMvc.perform(post("/api/v1/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid reset link."));
        }

        @Test
        @DisplayName("Password mismatch returns 400")
        void resetPassword_passwordMismatch_returns400() throws Exception {
            var request = Map.of(
                "token", UUID.randomUUID().toString(),
                "newPassword", "newpassword456",
                "confirmPassword", "differentpassword789"
            );

            mockMvc.perform(post("/api/v1/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password and confirmation do not match"));
        }
    }

    // ========== Helper Methods ==========

    /**
     * Registers a user via the API and manually verifies their email.
     */
    private void registerAndVerifyUser(String email, String password, String firstName) throws Exception {
        var request = Map.of(
            "email", email,
            "password", password,
            "firstName", firstName
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        // Manually verify the user's email (bypassing email flow for test speed)
        AppUser user = userRepository.findByEmail(email).orElseThrow();
        user.setEmailVerified(true);
        userRepository.save(user);
    }
}
