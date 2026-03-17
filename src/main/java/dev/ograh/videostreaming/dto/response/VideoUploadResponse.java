package dev.ograh.videostreaming.dto.response;

import dev.ograh.videostreaming.enums.VideoStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VideoUploadResponse(
        UUID videoId,
        String title,
        String description,
        String thumbnailUrl,
        String videoUrl,
        List<String> tags,
        long durationSecs,
        VideoStatus status,
        Instant createdAt
) {
}