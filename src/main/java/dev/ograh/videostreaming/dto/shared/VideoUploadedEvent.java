package dev.ograh.videostreaming.dto.shared;

import java.util.UUID;

public record VideoUploadedEvent(
        UUID videoId, VideoMetadata metadata, String originalVideoS3Key
) {
}