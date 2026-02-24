package org.jbelt.module.auth;

import org.jbelt.module.shared.email.EmailService;
import org.jbelt.module.user.internal.AppUser;
import org.jbelt.module.user.internal.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for REST API authentication endpoints.
 * <p>
 * Tests POST /api/v1/auth/register and POST /api/v1/auth/login endpoints.
 * Verifies AUTH-01 (registration), AUTH-04 (login), and SEC-01 (no user enumeration).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    // Mocked to prevent actual SMTP calls in tests (same pattern as EmailVerificationIntegrationTest)
    @MockitoBean
    private EmailService emailService;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    // ========== Registration Tests ==========

    @Test
    @DisplayName("Register with valid data returns 201 with verification message")
    void register_withValidData_returns201WithVerificationMessage() throws Exception {
        String uniqueEmail = "newuser-" + UUID.randomUUID() + "@example.com";

        var request = Map.of(
            "email", uniqueEmail,
            "password", "password123",
            "firstName", "New"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.message").value("Registration successful. Please check your email to verify your account."))
            .andExpect(jsonPath("$.email").value(uniqueEmail));
    }

    @Test
    @DisplayName("Register with duplicate email returns 409 Conflict")
    void register_withDuplicateEmail_returns409() throws Exception {
        String duplicateEmail = "duplicate-" + UUID.randomUUID() + "@example.com";

        // First registration should succeed
        registerUser(duplicateEmail, "password123", "First User");

        // Second registration with same email should fail
        var duplicateRequest = Map.of(
            "email", duplicateEmail,
            "password", "anotherpassword",
            "firstName", "Second"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(duplicateRequest)))
            .andExpect(status().isConflict())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.title").value("Duplicate Resource"));
    }

    @Test
    @DisplayName("Register with invalid email returns 400 Bad Request")
    void register_withInvalidEmail_returns400() throws Exception {
        var request = Map.of(
            "email", "not-an-email",
            "password", "password123",
            "firstName", "New"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Register with short password returns 400 Bad Request")
    void register_withShortPassword_returns400() throws Exception {
        var request = Map.of(
            "email", "shortpwd-" + UUID.randomUUID() + "@example.com",
            "password", "short",
            "firstName", "New"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(400));
    }

    // ========== Login Tests ==========

    @Test
    @DisplayName("Login with valid credentials returns 200 with JWT")
    void login_withValidCredentials_returns200WithJwt() throws Exception {
        String email = "logintest-" + UUID.randomUUID() + "@example.com";
        String password = "password123";
        registerAndVerifyUser(email, password, "Login Test User");

        var loginRequest = Map.of(
            "email", email,
            "password", password,
            "rememberMe", false
        );

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").isNumber())
            .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    @DisplayName("Login with rememberMe returns longer expiration")
    void login_withRememberMe_returnsLongerExpiration() throws Exception {
        String email = "rememberme-" + UUID.randomUUID() + "@example.com";
        String password = "password123";
        registerAndVerifyUser(email, password, "Remember Me Test User");

        // Login without rememberMe
        var shortLoginRequest = Map.of(
            "email", email,
            "password", password,
            "rememberMe", false
        );

        MvcResult shortResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(shortLoginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        // Login with rememberMe
        var longLoginRequest = Map.of(
            "email", email,
            "password", password,
            "rememberMe", true
        );

        MvcResult longResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(longLoginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        // Compare expiresIn values
        var shortResponse = jsonMapper.readTree(shortResult.getResponse().getContentAsString());
        var longResponse = jsonMapper.readTree(longResult.getResponse().getContentAsString());

        long shortExpiration = shortResponse.get("expiresIn").asLong();
        long longExpiration = longResponse.get("expiresIn").asLong();

        assertThat(longExpiration).isGreaterThan(shortExpiration);
    }

    @Test
    @DisplayName("Login with invalid password returns 401 Unauthorized")
    void login_withInvalidPassword_returns401() throws Exception {
        String email = "wrongpwd-" + UUID.randomUUID() + "@example.com";
        registerAndVerifyUser(email, "password123", "Wrong Password Test User");

        var loginRequest = Map.of(
            "email", email,
            "password", "wrongpassword",
            "rememberMe", false
        );

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Bad credentials"));
    }

    @Test
    @DisplayName("Login with nonexistent email returns 401 with same message (no user enumeration)")
    void login_withNonexistentEmail_returns401() throws Exception {
        var loginRequest = Map.of(
            "email", "nonexistent-" + UUID.randomUUID() + "@example.com",
            "password", "anypassword",
            "rememberMe", false
        );

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("Bad credentials"));
    }

    // ========== Helper Methods ==========

    /**
     * Registers a user AND manually verifies their email for use in login tests.
     * <p>
     * Since email verification blocks login (04-07), login tests need verified users.
     */
    private void registerAndVerifyUser(String email, String password, String firstName) throws Exception {
        registerUser(email, password, firstName);

        // Manually verify the user's email (bypassing email flow for test speed)
        AppUser user = userRepository.findByEmail(email).orElseThrow();
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    /**
     * Registers a user via the API (user will be unverified).
     */
    private void registerUser(String email, String password, String firstName) throws Exception {
        var request = Map.of(
            "email", email,
            "password", password,
            "firstName", firstName
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }
}
