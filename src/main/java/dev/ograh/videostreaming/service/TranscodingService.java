package dev.ograh.videostreaming.service;

import dev.ograh.videostreaming.dto.shared.VideoMetadata;
import dev.ograh.videostreaming.entity.TranscodingJob;
import dev.ograh.videostreaming.enums.EncodeFormat;
import dev.ograh.videostreaming.enums.VideoResolution;
import dev.ograh.videostreaming.utils.TranscodingHelperService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class TranscodingService {

    private static final Logger log = LoggerFactory.getLogger(TranscodingService.class);
    private final Executor transcodeExecutor;
    private final TranscodingHelperService transcodingHelperService;

    public void transcodeVideo(UUID videoId, String originalKey, VideoMetadata metadata) {

        CompletableFuture.runAsync(() -> {
            Path inputFile = null;
            try {
                inputFile = transcodingHelperService.downloadOriginalVideo(videoId, originalKey);

                for (EncodeFormat format : EncodeFormat.values()) {
                    if (format == EncodeFormat.ORIGINAL) continue;
                    for (VideoResolution resolution : VideoResolution.values()) {
                        if (resolution.getHeight() > metadata.dimension().height()) continue;

                        TranscodingJob job = transcodingHelperService.queueTranscodingJob(
                                videoId, format, resolution
                        );
                        transcodingHelperService.processTranscodingJob(job, metadata, inputFile);
                    }
                }
            } catch (Exception e) {
                log.error("Transcoding failed for video {}", videoId, e);
            } finally {
                transcodingHelperService.deleteSilently(inputFile);
            }
        }, transcodeExecutor);
    }
}