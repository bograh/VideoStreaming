package dev.ograh.videostreaming.dto.response;

public record CommentResponse(
        long id, String body, Long parentId, String createdAt, String authorName
) {
}