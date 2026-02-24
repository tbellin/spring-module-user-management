package org.jbelt.module.user;

import org.jbelt.module.shared.email.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the profile web pages (Thymeleaf).
 * <p>
 * Tests cover:
 * - GET /profile (authenticated profile page rendering)
 * - POST /profile (profile update with redirect)
 * - Anonymous access redirect to login
 * <p>
 * Uses .with(user(...)) for web chain tests and .with(csrf()) for POST requests.
 * EmailService is mocked to prevent actual SMTP calls.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ProfileWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailService emailService;

    // ========== GET /profile Tests ==========

    @Nested
    @DisplayName("GET /profile")
    class GetProfilePageTests {

        @Test
        @DisplayName("Authenticated user gets 200 with profile page content")
        void profilePage_authenticated_returns200() throws Exception {
            // Use seed admin user
            String email = "tizianobellin@yahoo.com";

            mockMvc.perform(get("/profile")
                    .with(user(email).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("profile")));
        }

        @Test
        @DisplayName("Anonymous user is redirected to /login")
        void profilePage_anonymous_redirectsToLogin() throws Exception {
            mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        }
    }

    // ========== POST /profile Tests ==========

    @Nested
    @DisplayName("POST /profile")
    class UpdateProfilePageTests {

        @Test
        @DisplayName("Authenticated POST with valid params redirects to /profile")
        void updateProfile_authenticated_redirects() throws Exception {
            // Use seed admin user
            String email = "tizianobellin@yahoo.com";

            mockMvc.perform(post("/profile")
                    .with(user(email).roles("ADMIN"))
                    .with(csrf())
                    .param("displayName", "Updated Display")
                    .param("firstName", "Updated")
                    .param("lastName", "Name"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
        }
    }
}
