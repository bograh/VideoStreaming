package dev.ograh.videostreaming.dto.projection;

import java.util.UUID;

public interface VideoTagProjection {
    UUID getVideoId();

    String getTag();
}