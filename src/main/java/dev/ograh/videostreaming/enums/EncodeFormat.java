package dev.ograh.videostreaming.enums;

public enum EncodeFormat {
    ORIGINAL, H264, H265;

    public String getFfmpegCodec() {
        return switch (this) {
            case H264 -> "libx264";
            case H265 -> "libx265";
            case ORIGINAL -> null;
        };
    }

    public int getPriority() {
        return switch (this) {
            case H264 -> 1;
            case H265 -> 2;
            case ORIGINAL -> 0;
        };
    }
}