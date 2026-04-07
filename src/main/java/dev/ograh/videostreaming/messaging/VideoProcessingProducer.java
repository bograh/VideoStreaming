package dev.ograh.videostreaming.messaging;

import dev.ograh.videostreaming.dto.shared.VideoMetadata;
import dev.ograh.videostreaming.dto.shared.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoProcessingProducer {

    public void publishVideoUploadedEvent(UUID videoId, VideoMetadata metadata, String originalVideoS3Key) {
        VideoUploadedEvent event = new VideoUploadedEvent(videoId, metadata, originalVideoS3Key);
    }

}