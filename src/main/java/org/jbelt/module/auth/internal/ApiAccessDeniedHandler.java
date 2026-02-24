package org.jbelt.module.auth.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;

/**
 * Custom access denied handler for API endpoints.
 * <p>
 * Returns JSON ProblemDetail (RFC 9457) responses for 403 Forbidden errors.
 * This handler is used by the API SecurityFilterChain, NOT the web chain.
 * <p>
 * IMPORTANT: Security exceptions bypass @ControllerAdvice, so we need this
 * custom handler to produce JSON responses for API endpoints.
 */
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Forbidden");
        problem.setDetail("You do not have permission to access this resource");
        problem.setInstance(URI.create(request.getRequestURI()));

        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
