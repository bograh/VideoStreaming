package dev.ograh.videostreaming.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name cannot be blank")
        @Size(min = 3, max = 36, message = "Name must be between 3 and 36 characters")
        String name,

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, max = 256, message = "Password must be between 8 and 256 characters")
        String password
) {
}