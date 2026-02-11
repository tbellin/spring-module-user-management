package com.example.usermanagement.auth;

import com.example.usermanagement.shared.email.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for web password management pages.
 * <p>
 * Tests cover:
 * - GET/POST /change-password (authenticated)
 * - GET/POST /forgot-password (public)
 * - GET /reset-password (public, token-based)
 * - Login page forgot-password link
 * - Navbar dropdown for authenticated users
 * <p>
 * EmailService is mocked to prevent actual SMTP calls while keeping
 * the JavaMailSender bean intact for actuator health checks.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PasswordWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailService emailService;

    // ========== Change Password Page Tests ==========

    @Nested
    @DisplayName("Change Password Page")
    class ChangePasswordPageTests {

        @Test
        @DisplayName("Authenticated user can access change password page")
        void changePasswordPage_authenticated_returns200() throws Exception {
            String email = "webchg-" + UUID.randomUUID() + "@example.com";

            mockMvc.perform(get("/change-password")
                    .with(user(email).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Change Password")));
        }

        @Test
        @DisplayName("Unauthenticated user is redirected to login")
        void changePasswordPage_unauthenticated_redirectsToLogin() throws Exception {
            mockMvc.perform(get("/change-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        }
    }

    // ========== Forgot Password Page Tests ==========

    @Nested
    @DisplayName("Forgot Password Page")
    class ForgotPasswordPageTests {

        @Test
        @DisplayName("Forgot password page is accessible without auth")
        void forgotPasswordPage_returns200() throws Exception {
            mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Forgot Password")));
        }

        @Test
        @DisplayName("POST forgot-password shows generic message")
        void forgotPasswordPost_showsMessage() throws Exception {
            mockMvc.perform(post("/forgot-password")
                    .with(csrf())
                    .param("email", "anyone-" + UUID.randomUUID() + "@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                    "If an account exists with this email")));
        }
    }

    // ========== Reset Password Page Tests ==========

    @Nested
    @DisplayName("Reset Password Page")
    class ResetPasswordPageTests {

        @Test
        @DisplayName("Reset password page with token renders form with hidden token")
        void resetPasswordPage_withToken_returns200() throws Exception {
            mockMvc.perform(get("/reset-password")
                    .param("token", "abc123"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Reset Password")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("abc123")));
        }
    }

    // ========== Login Page Integration Tests ==========

    @Nested
    @DisplayName("Login Page Integration")
    class LoginPageTests {

        @Test
        @DisplayName("Login page contains forgot-password link")
        void loginPage_hasForgotPasswordLink() throws Exception {
            mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("forgot-password")));
        }
    }

    // ========== Navbar Tests ==========

    @Nested
    @DisplayName("Navbar Dropdown")
    class NavbarTests {

        @Test
        @DisplayName("Authenticated user sees navbar dropdown with Change Password")
        void navbar_authenticated_hasDropdown() throws Exception {
            String email = "navuser-" + UUID.randomUUID() + "@example.com";

            mockMvc.perform(get("/")
                    .with(user(email).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Change Password")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("dropdown")));
        }
    }
}
