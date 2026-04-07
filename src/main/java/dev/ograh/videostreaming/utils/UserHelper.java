package dev.ograh.videostreaming.utils;

import dev.ograh.videostreaming.entity.User;
import dev.ograh.videostreaming.enums.UserRole;
import dev.ograh.videostreaming.exception.ForbiddenException;
import dev.ograh.videostreaming.repository.UserRepository;
import dev.ograh.videostreaming.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class UserHelper {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public UserHelper(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    private String getUserEmailFromAuthHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            return null;
        }
        String token = bearerToken.substring(7);
        return jwtService.extractEmail(token);
    }

    public User getAuthenticatedUser(HttpServletRequest request) {
        String email = getUserEmailFromAuthHeader(request);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ForbiddenException("You are not authorized to upload a video."));

        if (user.getRole() != UserRole.CREATOR) {
            user.setRole(UserRole.CREATOR);
            userRepository.save(user);
        }

        return user;
    }
}