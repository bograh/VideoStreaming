package dev.ograh.videostreaming.dto.response;

public record UserResponse(
        String id,
        String name,
        String email,
        String role
) {
}