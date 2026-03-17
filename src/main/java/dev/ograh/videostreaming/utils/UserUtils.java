package dev.ograh.videostreaming.utils;

import dev.ograh.videostreaming.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class UserUtils {

    private final JwtService jwtService;

    public UserUtils(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public String getUserEmailFromAuthHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            return null;
        }
        String token = bearerToken.substring(7);
        return jwtService.extractEmail(token);
    }
}