package dev.ograh.videostreaming.dto.projection;

import dev.ograh.videostreaming.enums.VideoStatus;

import java.time.Instant;
import java.util.UUID;

public interface VideoProjection {

    UUID getId();

    String getTitle();

    String getDescription();

    String getThumbnailKey();

    long getDurationSecs();

    VideoStatus getStatus();

    long getViewCount();

    long getLikeCount();

    long getDislikeCount();

    Instant getCreatedAt();
}