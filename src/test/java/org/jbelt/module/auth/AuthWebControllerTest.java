package org.jbelt.module.auth;

import org.jbelt.module.shared.email.EmailService;
import org.jbelt.module.user.internal.AppUser;
import org.jbelt.module.user.internal.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for web authentication endpoints.
 * <p>
 * Tests Thymeleaf-based registration, login, and logout flows.
 * Verifies AUTH-01 (registration), AUTH-04 (login), AUTH-05 (logout).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailService emailService;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    // ========== GET Pages Tests ==========

    @Test
    @DisplayName("GET /login returns login view")
    void getLoginPage_returnsLoginView() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/login"));
    }

    @Test
    @DisplayName("GET /register returns register view with empty form")
    void getRegisterPage_returnsRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/register"))
            .andExpect(model().attributeExists("registrationForm"));
    }

    // ========== Registration Tests ==========

    @Test
    @DisplayName("POST /register with valid data redirects to login with verification toast")
    void register_withValidData_redirectsToLogin() throws Exception {
        String uniqueEmail = "webuser-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/register")
                .with(csrf())
                .param("email", uniqueEmail)
                .param("password", "password123")
                .param("firstName", "Web")
                .param("lastName", "User"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"))
            .andExpect(flash().attributeExists("toast"));
    }

    @Test
    @DisplayName("POST /register with duplicate email returns register view with error")
    void register_withDuplicateEmail_returnsRegisterViewWithError() throws Exception {
        String duplicateEmail = "webdupe-" + UUID.randomUUID() + "@example.com";

        // First registration via API (faster than form submission)
        registerUserViaApi(duplicateEmail, "password123", "First User");

        // Second registration via web form should fail
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("email", duplicateEmail)
                .param("password", "anotherpassword")
                .param("firstName", "Second")
                .param("lastName", "User"))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/register"))
            .andExpect(model().hasErrors())
            .andExpect(model().attributeHasFieldErrors("registrationForm", "email"));
    }

    @Test
    @DisplayName("POST /register with invalid data returns register view with validation errors")
    void register_withInvalidData_returnsRegisterViewWithErrors() throws Exception {
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("email", "")  // empty email
                .param("password", "short")  // too short
                .param("firstName", ""))  // empty first name
            .andExpect(status().isOk())
            .andExpect(view().name("auth/register"))
            .andExpect(model().hasErrors());
    }

    // ========== Login Tests ==========

    @Test
    @DisplayName("POST /login with valid credentials redirects to home")
    void login_withValidCredentials_redirectsToHome() throws Exception {
        String email = "weblogin-" + UUID.randomUUID() + "@example.com";
        String password = "password123";
        registerAndVerifyUser(email, password, "Web Login User");

        // Spring Security form login uses "username" parameter (email is our username)
        mockMvc.perform(post("/login")
                .with(csrf())
                .param("username", email)
                .param("password", password))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("POST /login with invalid credentials redirects to /login?error")
    void login_withInvalidCredentials_redirectsToLoginWithError() throws Exception {
        mockMvc.perform(post("/login")
                .with(csrf())
                .param("username", "nonexistent@example.com")
                .param("password", "wrongpassword"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"));
    }

    // ========== Logout Tests ==========

    @Test
    @DisplayName("POST /logout redirects to /login?logout")
    void logout_redirectsToLoginWithLogout() throws Exception {
        // Note: Spring Security logout works without being logged in
        // It clears any existing session and redirects
        mockMvc.perform(post("/logout")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?logout"));
    }

    // ========== Helper Methods ==========

    /**
     * Registers a user via the API for use in subsequent web tests.
     */
    private void registerUserViaApi(String email, String password, String firstName) throws Exception {
        var request = Map.of(
            "email", email,
            "password", password,
            "firstName", firstName
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType("application/json")
                .content(jsonMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }

    /**
     * Registers a user via the API and manually verifies their email.
     * Required for login tests since email verification gate (Phase 4) blocks unverified users.
     */
    private void registerAndVerifyUser(String email, String password, String firstName) throws Exception {
        registerUserViaApi(email, password, firstName);

        AppUser user = userRepository.findByEmail(email).orElseThrow();
        user.setEmailVerified(true);
        userRepository.save(user);
    }
}
