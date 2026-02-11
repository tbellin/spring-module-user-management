package com.example.usermanagement.user;

import com.example.usermanagement.shared.email.EmailService;
import com.example.usermanagement.user.internal.AppUser;
import com.example.usermanagement.user.internal.UserRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the profile REST API endpoints.
 * <p>
 * Tests cover:
 * - GET /api/v1/users/me (authenticated profile retrieval)
 * - PUT /api/v1/users/me (authenticated profile update)
 * <p>
 * Uses SecurityMockMvcRequestPostProcessors.user() for API chain tests (stateless, JWT).
 * EmailService is mocked to prevent actual SMTP calls.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ProfileApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailService emailService;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    // ========== GET /api/v1/users/me Tests ==========

    @Nested
    @DisplayName("GET /api/v1/users/me")
    class GetProfileTests {

        @Test
        @DisplayName("Authenticated user gets 200 with profile JSON containing email, roles, createdAt")
        void getProfile_authenticated_returns200WithUserJson() throws Exception {
            // Use the seed admin user from V2__seed_dev_data.sql
            String email = "tizianobellin@yahoo.com";

            mockMvc.perform(get("/api/v1/users/me")
                    .with(user(email).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("Unauthenticated request returns 401")
        void getProfile_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/users/me")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        }
    }

    // ========== PUT /api/v1/users/me Tests ==========

    @Nested
    @DisplayName("PUT /api/v1/users/me")
    class UpdateProfileTests {

        @Test
        @DisplayName("Authenticated user can update profile and gets 200 with updated JSON")
        void updateProfile_authenticated_returns200WithUpdatedJson() throws Exception {
            String email = "profupd-" + UUID.randomUUID() + "@test.com";
            registerAndVerifyUser(email, "password123", "Prof Update");

            var request = Map.of(
                "displayName", "Updated Name",
                "firstName", "Updated",
                "lastName", "User"
            );

            mockMvc.perform(put("/api/v1/users/me")
                    .with(user(email).roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("User"));
        }

        @Test
        @DisplayName("Unauthenticated request returns 401")
        void updateProfile_unauthenticated_returns401() throws Exception {
            var request = Map.of(
                "displayName", "Test",
                "firstName", "Test",
                "lastName", "User"
            );

            mockMvc.perform(put("/api/v1/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }
    }

    // ========== Helper Methods ==========

    /**
     * Registers a user via the API and manually verifies their email.
     */
    private void registerAndVerifyUser(String email, String password, String displayName) throws Exception {
        var request = Map.of(
            "email", email,
            "password", password,
            "displayName", displayName
        );
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        AppUser user = userRepository.findByEmail(email).orElseThrow();
        user.setEmailVerified(true);
        userRepository.save(user);
    }
}
