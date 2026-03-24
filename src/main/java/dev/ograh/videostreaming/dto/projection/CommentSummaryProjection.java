package dev.ograh.videostreaming.dto.projection;

import java.time.Instant;

public interface CommentSummaryProjection {

    Long getId();

    String getComment();

    Long getLikeCount();

    boolean getPinned();

    boolean getHidden();

    Instant getCreatedAt();

    String getAuthor();

    Long getParentCommentId();

}