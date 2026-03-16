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

}