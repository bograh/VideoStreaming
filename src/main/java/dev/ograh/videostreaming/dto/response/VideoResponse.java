package dev.ograh.videostreaming.dto.response;

import dev.ograh.videostreaming.enums.VideoStatus;

import java.util.List;

public record VideoResponse(
        String videoId,
        String title,
        String description,
        String thumbnailUrl,
        List<VideoResolutionsUrl> videoUrls,
        List<String> tags,
        long durationSecs,
        VideoStatus status,
        long views,
        long likeCount,
        long dislikeCount,
        String createdAt
) {
}