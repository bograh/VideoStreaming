package dev.ograh.videostreaming.enums;

import lombok.Getter;

@Getter
public enum CommentSortField {
    CREATED_AT("createdAt"),
    LIKE_COUNT("likeCount");

    private final String field;

    CommentSortField(String field) {
        this.field = field;
    }

    public static CommentSortField from(String input) {

        if (input == null || input.isBlank()) {
            return CREATED_AT;
        }

        String normalized = input.replace("_", "").toLowerCase();

        for (CommentSortField field : values()) {
            String enumNormalized = field.name().replace("_", "").toLowerCase();
            if (enumNormalized.equals(normalized)) {
                return field;
            }
        }

        return CREATED_AT;
    }
}