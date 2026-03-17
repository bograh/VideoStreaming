package dev.ograh.videostreaming.dto.shared;

import dev.ograh.videostreaming.enums.Resolution;

public record VideoMetadata(
        int durationSecs,
        int fps,
        int bitrate,
        Resolution resolution,
        VideoDimension dimension
) {
}