package dev.ograh.videostreaming.enums;

public enum EncodeFormat {
    ORIGINAL, H264, AV1, MPEG4;

    public String getFfmpegCodec() {
        return switch (this) {
            case H264 -> "libx264";
            case AV1 -> "libaom-av1";
            case MPEG4 -> "mpeg4";
            case ORIGINAL -> null;
        };
    }

    public int getPriority() {
        return switch (this) {
            case H264 -> 1;
            case AV1 -> 3;
            case MPEG4 -> 4;
            case ORIGINAL -> 0;
        };
    }
}