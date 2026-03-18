package dev.ograh.videostreaming.enums;

import lombok.Getter;

@Getter
public enum VideoResolution {
    R2160P(3840, 2160),
    R1440P(2560, 1440),
    R1080P(1920, 1080),
    R720P(1280, 720),
    R480P(854, 480),
    R360P(640, 360),
    R240P(426, 240),
    R144P(256, 144);

    private final int width;
    private final int height;

    VideoResolution(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public String toScaleFilter() {
        return String.format("scale=%d:%d:force_original_aspect_ratio=decrease,"
                + "pad=%d:%d:(ow-iw)/2:(oh-ih)/2", width, height, width, height);
    }
}