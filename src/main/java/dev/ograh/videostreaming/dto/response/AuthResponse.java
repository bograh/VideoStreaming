package dev.ograh.videostreaming.dto.response;

public record AuthResponse(
        String token,
        UserResponse user
) {
}