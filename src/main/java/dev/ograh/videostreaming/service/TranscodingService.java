package dev.ograh.videostreaming.service;

import dev.ograh.videostreaming.dto.shared.VideoMetadata;
import dev.ograh.videostreaming.entity.TranscodingJob;
import dev.ograh.videostreaming.enums.EncodeFormat;
import dev.ograh.videostreaming.enums.Resolution;
import dev.ograh.videostreaming.utils.TranscodingHelperService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class TranscodingService {

    private static final Logger log = LoggerFactory.getLogger(TranscodingService.class);
    private final Executor transcodeExecutor;
    private final TranscodingHelperService transcodingHelperService;

    public void transcodeVideo(
            UUID videoId, VideoMetadata metadata, Path inputFile
    ) {

        List<EncodeFormat> formats = transcodingHelperService.getTargetFormats();
        List<Resolution> resolutions = transcodingHelperService.getTargetResolutions(metadata);

        for (EncodeFormat format : formats) {
            for (Resolution resolution : resolutions) {
                if (!transcodingHelperService.shouldProcess(format, resolution)) {
                    continue;
                }

                CompletableFuture.runAsync(() -> {
                    try {
                        TranscodingJob job = transcodingHelperService.queueTranscodingJob(
                                videoId, format, resolution
                        );

                        transcodingHelperService.processTranscodingJob(
                                job, metadata, inputFile
                        );

                    } catch (Exception e) {
                        log.error("Failed job for {} {}", format, resolution, e);
                    }
                }, transcodeExecutor);
            }
        }
    }
}