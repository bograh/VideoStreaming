package dev.ograh.videostreaming.messaging;

import dev.ograh.videostreaming.dto.shared.VideoUploadedEvent;
import dev.ograh.videostreaming.service.TranscodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VideoProcessingConsumer {

    private final TranscodingService transcodingService;

    public void process(VideoUploadedEvent event) {
    }

}