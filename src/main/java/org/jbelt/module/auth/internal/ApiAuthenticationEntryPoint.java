package org.jbelt.module.auth.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;

/**
 * Custom authentication entry point for API endpoints.
 * <p>
 * Returns JSON ProblemDetail (RFC 9457) responses for 401 Unauthorized errors.
 * This handler is used by the API SecurityFilterChain, NOT the web chain.
 * <p>
 * IMPORTANT: Security exceptions bypass @ControllerAdvice, so we need this
 * custom handler to produce JSON responses for API endpoints.
 */
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Unauthorized");
        problem.setDetail("Authentication is required to access this resource");
        problem.setInstance(URI.create(request.getRequestURI()));

        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
