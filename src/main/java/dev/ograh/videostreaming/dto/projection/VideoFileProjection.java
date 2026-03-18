package dev.ograh.videostreaming.dto.projection;

import dev.ograh.videostreaming.enums.Resolution;

public interface VideoFileProjection {

    Resolution getResolution();

    String getFileKey();
}