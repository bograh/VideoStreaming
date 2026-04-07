package dev.ograh.videostreaming.dto.response;

import java.util.List;

public record VideoSummaryResponse(
        String videoId,
        String title,
        String creator,
        long durationSecs,
        String thumbnailUrl,
        long views,
        String createdAt,
        List<String> tags
) {
}