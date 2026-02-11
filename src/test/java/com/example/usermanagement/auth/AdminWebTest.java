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
 * Integration tests for the admin web pages (Thymeleaf).
 * <p>
 * Tests cover:
 * - GET /admin/users (admin user list page, authorization)
 * - GET /admin/users/new (create user form)
 * - POST /admin/users/new (create user submission)
 * - Access control: USER role gets 403, anonymous redirects to login
 * <p>
 * Uses .with(user(...)) for web chain tests and .with(csrf()) for POST requests.
 * EmailService is mocked to prevent actual SMTP calls during invite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AdminWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailService emailService;

    // ========== GET /admin/users Tests ==========

    @Nested
    @DisplayName("GET /admin/users")
    class ListUsersPageTests {

        @Test
        @DisplayName("ADMIN gets 200 with admin user list page")
        void listUsersPage_admin_returns200() throws Exception {
            mockMvc.perform(get("/admin/users")
                    .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("admin")));
        }

        @Test
        @DisplayName("USER role gets 403")
        void listUsersPage_user_returns403() throws Exception {
            mockMvc.perform(get("/admin/users")
                    .with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Anonymous user is redirected to login")
        void listUsersPage_anonymous_redirectsToLogin() throws Exception {
            mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        }
    }

    // ========== GET /admin/users/new Tests ==========

    @Nested
    @DisplayName("GET /admin/users/new")
    class CreateUserFormTests {

        @Test
        @DisplayName("ADMIN gets 200 with create user form")
        void createUserForm_admin_returns200() throws Exception {
            mockMvc.perform(get("/admin/users/new")
                    .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("email")));
        }
    }

    // ========== POST /admin/users/new Tests ==========

    @Nested
    @DisplayName("POST /admin/users/new")
    class CreateUserSubmitTests {

        @Test
        @DisplayName("ADMIN creates user and is redirected to /admin/users")
        void createUser_admin_redirects() throws Exception {
            String email = "webinvite-" + UUID.randomUUID() + "@test.com";

            mockMvc.perform(post("/admin/users/new")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .with(csrf())
                    .param("email", email)
                    .param("role", "ROLE_USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
        }
    }
}
