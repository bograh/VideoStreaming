package dev.ograh.videostreaming.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ograh.videostreaming.exception.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class CustomAccessDenied implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    public CustomAccessDenied(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpServletResponse.SC_FORBIDDEN,
                "FORBIDDEN",
                "You don't have the required permission to access this resource",
                request.getRequestURI(),
                String.valueOf(LocalDateTime.now())
        );
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}