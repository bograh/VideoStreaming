package dev.ograh.videostreaming.enums;

import lombok.Getter;

@Getter
public enum VideoSortField {

    TITLE("title"),
    VIEW_COUNT("viewCount"),
    LIKE_COUNT("likeCount"),
    DISLIKE_COUNT("dislikeCount"),
    CREATED_AT("createdAt");

    private final String field;

    VideoSortField(String field) {
        this.field = field;
    }

    public static VideoSortField from(String input) {

        if (input == null || input.isBlank()) {
            return CREATED_AT;
        }

        String normalized = input.replace("_", "").toLowerCase();

        for (VideoSortField field : values()) {
            String enumNormalized = field.name().replace("_", "").toLowerCase();
            if (enumNormalized.equals(normalized)) {
                return field;
            }
        }

        return CREATED_AT;
    }
}