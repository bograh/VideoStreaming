package dev.ograh.videostreaming.enums;

import lombok.Getter;

@Getter
public enum Resolution {

    R2160P("2160p", 3840, 2160),
    R1440P("1440p", 2560, 1440),
    R1080P("1080p", 1920, 1080),
    R720P("720p", 1280, 720),
    R480P("480p", 854, 480),
    R360P("360p", 640, 360),
    R240P("240p", 426, 240),
    R144P("144p", 256, 144),
    AUDIO_ONLY("audio", 0, 0);

    private final String label;
    private final int width;
    private final int height;

    Resolution(String label, int width, int height) {
        this.label = label;
        this.width = width;
        this.height = height;
    }

    public String toScaleFilter() {
        if (this == AUDIO_ONLY) {
            return "";
        }

        return String.format(
                "scale=%d:%d:force_original_aspect_ratio=decrease," +
                        "pad=%d:%d:(ow-iw)/2:(oh-ih)/2",
                width, height, width, height
        );
    }

    public static Resolution fromHeight(int height) {
        if (height >= 2160) return R2160P;
        if (height >= 1440) return R1440P;
        if (height >= 1080) return R1080P;
        if (height >= 720) return R720P;
        if (height >= 480) return R480P;
        if (height >= 360) return R360P;
        if (height >= 240) return R240P;
        if (height >= 144) return R144P;
        return AUDIO_ONLY;
    }
}