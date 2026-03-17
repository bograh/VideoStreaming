package dev.ograh.videostreaming.enums;

import lombok.Getter;

@Getter
public enum Resolution {
    R2160P("2160p"),
    R1440P("1440p"),
    R1080P("1080p"),
    R720P("720p"),
    R480P("480p"),
    R360P("360p"),
    R240P("240p"),
    AUDIO_ONLY("audio");

    private final String label;

    Resolution(String label) {
        this.label = label;
    }

    public static Resolution fromHeight(int height) {
        if (height >= 2160) return R2160P;
        if (height >= 1440) return R1440P;
        if (height >= 1080) return R1080P;
        if (height >= 720) return R720P;
        if (height >= 480) return R480P;
        if (height >= 360) return R360P;
        if (height >= 240) return R240P;
        return AUDIO_ONLY;
    }

}