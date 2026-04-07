package dev.ograh.videostreaming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record VideoUploadRequest(
        @NotBlank(message = "Video title cannot be blank")
        String title,

        @NotBlank(message = "Video description cannot be blank")
        @Size(min = 10, max = 5000, message = "Video description must be between 10 and 6000 characters")
        String description,

        List<String> tags
) {
}