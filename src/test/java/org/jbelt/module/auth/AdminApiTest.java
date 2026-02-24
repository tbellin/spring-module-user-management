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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the admin REST API endpoints.
 * <p>
 * Tests cover:
 * - GET /api/v1/admin/users (list with pagination, authorization)
 * - POST /api/v1/admin/users (create/invite user)
 * - PUT /api/v1/admin/users/{id} (update user)
 * - PATCH /api/v1/admin/users/{id}/status (toggle enable/disable, self-disable prevention)
 * <p>
 * Uses SecurityMockMvcRequestPostProcessors.user() for API chain tests.
 * EmailService is mocked to prevent actual SMTP calls during invite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AdminApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailService emailService;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    // ========== GET /api/v1/admin/users Tests ==========

    @Nested
    @DisplayName("GET /api/v1/admin/users")
    class ListUsersTests {

        @Test
        @DisplayName("ADMIN role gets 200 with paginated JSON")
        void listUsers_admin_returns200WithPaginatedJson() throws Exception {
            mockMvc.perform(get("/api/v1/admin/users")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.pageable").exists());
        }

        @Test
        @DisplayName("USER role gets 403")
        void listUsers_user_returns403() throws Exception {
            mockMvc.perform(get("/api/v1/admin/users")
                    .with(user("user@example.com").roles("USER"))
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated request gets 401")
        void listUsers_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/admin/users")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        }
    }

    // ========== POST /api/v1/admin/users Tests ==========

    @Nested
    @DisplayName("POST /api/v1/admin/users")
    class CreateUserTests {

        @Test
        @DisplayName("ADMIN creates user and gets 201 with user DTO")
        void createUser_admin_returns201() throws Exception {
            String email = "invite-" + UUID.randomUUID() + "@test.com";

            var request = Map.of(
                "email", email,
                "roles", List.of("ROLE_USER")
            );

            mockMvc.perform(post("/api/v1/admin/users")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.roles").isArray())
                .andExpect(jsonPath("$.setPasswordUrl").isString());
        }

        @Test
        @DisplayName("Duplicate email returns 409")
        void createUser_duplicateEmail_returns409() throws Exception {
            // Use the seed user email which already exists
            String email = "tizianobellin@yahoo.com";

            var request = Map.of(
                "email", email,
                "roles", List.of("ROLE_USER")
            );

            mockMvc.perform(post("/api/v1/admin/users")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
        }
    }

    // ========== PUT /api/v1/admin/users/{id} Tests ==========

    @Nested
    @DisplayName("PUT /api/v1/admin/users/{id}")
    class UpdateUserTests {

        @Test
        @DisplayName("ADMIN updates user and gets 200")
        void updateUser_admin_returns200() throws Exception {
            // Create a user to update
            String email = "toupdate-" + UUID.randomUUID() + "@test.com";
            createTestUser(email);

            AppUser user = userRepository.findByEmail(email).orElseThrow();

            var request = Map.of(
                "firstName", "NewFirst",
                "lastName", "NewLast",
                "roles", List.of("ROLE_USER")
            );

            mockMvc.perform(put("/api/v1/admin/users/" + user.getId())
                    .with(user("admin@example.com").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("NewFirst"))
                .andExpect(jsonPath("$.lastName").value("NewLast"));
        }
    }

    // ========== PATCH /api/v1/admin/users/{id}/status Tests ==========

    @Nested
    @DisplayName("PATCH /api/v1/admin/users/{id}/status")
    class ToggleStatusTests {

        @Test
        @DisplayName("ADMIN can disable another user and gets 200")
        void toggleStatus_admin_returns200() throws Exception {
            String email = "todisable-" + UUID.randomUUID() + "@test.com";
            createTestUser(email);

            AppUser user = userRepository.findByEmail(email).orElseThrow();

            var request = Map.of("enabled", false);

            mockMvc.perform(patch("/api/v1/admin/users/" + user.getId() + "/status")
                    .with(user("admin@example.com").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
        }

        @Test
        @DisplayName("Self-disable returns 400")
        void toggleStatus_selfDisable_returns400() throws Exception {
            // Use the seed admin user
            String adminEmail = "tizianobellin@yahoo.com";
            AppUser admin = userRepository.findByEmail(adminEmail).orElseThrow();

            var request = Map.of("enabled", false);

            mockMvc.perform(patch("/api/v1/admin/users/" + admin.getId() + "/status")
                    .with(user(adminEmail).roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Cannot disable your own account"));
        }
    }

    // ========== Helper Methods ==========

    /**
     * Creates a verified test user via the registration API.
     */
    private void createTestUser(String email) throws Exception {
        var request = Map.of(
            "email", email,
            "password", "password123",
            "firstName", "Test"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        AppUser user = userRepository.findByEmail(email).orElseThrow();
        user.setEmailVerified(true);
        userRepository.save(user);
    }
}
