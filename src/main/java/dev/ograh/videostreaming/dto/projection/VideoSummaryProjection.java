package dev.ograh.videostreaming.dto.projection;

import java.time.Instant;
import java.util.UUID;

public interface VideoSummaryProjection {

    UUID getId();

    String getTitle();

    String getUser();

    long getDurationSecs();

    String getThumbnailKey();

    long getViewCount();

    Instant getCreatedAt();
}