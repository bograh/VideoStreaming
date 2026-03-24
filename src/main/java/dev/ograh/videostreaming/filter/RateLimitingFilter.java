package dev.ograh.videostreaming.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ograh.videostreaming.exception.ErrorResponse;
import dev.ograh.videostreaming.service.RateLimitService;
import dev.ograh.videostreaming.utils.UserHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final UserHelper userHelper;
    private final ObjectMapper objectMapper;

    private static final int UPLOAD_LIMIT = 12;
    private static final int AUTH_LIMIT = 10;

    private static final long WINDOW_MS = 60000;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!isRateLimitedEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = getUserIdentifier(request);
        String key = String.format("rate_limit:%s:%s", userId, path);
        int limit = resolveLimit(path);

        boolean allowed = rateLimitService.isAllowed(key, limit, WINDOW_MS);

        if (!allowed) {
            ErrorResponse errorResponse = new ErrorResponse(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                    "Rate limit exceeded",
                    request.getRequestURI(),
                    LocalDateTime.now().toString(),
                    UUID.randomUUID().toString()
            );
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }
        filterChain.doFilter(request, response);

    }

    private boolean isRateLimitedEndpoint(String path) {
        return path.startsWith("/api/videos/upload") || path.startsWith("/api/auth");
    }

    private int resolveLimit(String path) {
        if (path.startsWith("/api/videos/upload")) {
            return UPLOAD_LIMIT;
        }
        return AUTH_LIMIT;
    }

    private String getUserIdentifier(HttpServletRequest request) {
        try {
            return userHelper.getAuthenticatedUser(request).getEmail();
        } catch (Exception e) {
            return request.getRemoteAddr();
        }
    }
}