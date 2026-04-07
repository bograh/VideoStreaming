package dev.ograh.videostreaming.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CommentRequest(
        @NotBlank(message = "Comment body cannot be blank")
        String body,

        long parentCommentId
) {
}