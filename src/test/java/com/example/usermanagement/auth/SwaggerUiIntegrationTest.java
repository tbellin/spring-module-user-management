package com.example.usermanagement.auth;

import com.example.usermanagement.shared.email.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Swagger UI accessibility and OpenAPI spec correctness.
 * <p>
 * Verifies:
 * - Swagger UI is accessible without authentication
 * - OpenAPI spec is accessible without authentication
 * - All API paths are present in the spec
 * - Web paths are excluded from the spec
 * - JWT security scheme is properly defined
 * <p>
 * EmailService is mocked to prevent actual SMTP calls (per 06-04 convention).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SwaggerUiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailService emailService;

    @Test
    @DisplayName("Swagger UI is accessible without authentication")
    void swaggerUiIsAccessibleWithoutAuth() throws Exception {
        // GET /swagger-ui/index.html serves the Swagger UI HTML page directly (200).
        // The key assertion: the response is NOT 401/403 (security allows access).
        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    @DisplayName("OpenAPI spec is accessible without authentication and returns JSON")
    void openApiSpecIsAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.info.title").value("User Management API"));
    }

    @Test
    @DisplayName("OpenAPI spec contains all expected API paths")
    void openApiSpecContainsAllApiPaths() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/auth/register']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/auth/login']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/auth/resend-verification']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/auth/change-password']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/auth/forgot-password']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/auth/reset-password']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/users/me']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/admin/users']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/admin/users/{id}']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/admin/users/{id}/status']").exists());
    }

    @Test
    @DisplayName("OpenAPI spec does not contain web paths")
    void openApiSpecDoesNotContainWebPaths() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/']").doesNotExist())
            .andExpect(jsonPath("$.paths['/login']").doesNotExist())
            .andExpect(jsonPath("$.paths['/register']").doesNotExist())
            .andExpect(jsonPath("$.paths['/admin/users']").doesNotExist());
    }

    @Test
    @DisplayName("OpenAPI spec contains JWT security scheme")
    void openApiSpecContainsJwtSecurityScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.components.securitySchemes['Bearer Authentication'].type").value("http"))
            .andExpect(jsonPath("$.components.securitySchemes['Bearer Authentication'].scheme").value("bearer"))
            .andExpect(jsonPath("$.components.securitySchemes['Bearer Authentication'].bearerFormat").value("JWT"));
    }
}
