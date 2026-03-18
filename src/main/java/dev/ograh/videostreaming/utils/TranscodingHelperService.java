package dev.ograh.videostreaming.utils;

import dev.ograh.videostreaming.dto.shared.UploadResult;
import dev.ograh.videostreaming.dto.shared.VideoMetadata;
import dev.ograh.videostreaming.entity.TranscodingJob;
import dev.ograh.videostreaming.entity.Video;
import dev.ograh.videostreaming.entity.VideoFile;
import dev.ograh.videostreaming.enums.EncodeFormat;
import dev.ograh.videostreaming.enums.JobStatus;
import dev.ograh.videostreaming.enums.Resolution;
import dev.ograh.videostreaming.repository.TranscodingJobRepository;
import dev.ograh.videostreaming.repository.VideoFileRepository;
import dev.ograh.videostreaming.repository.VideoRepository;
import dev.ograh.videostreaming.service.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

@Component
public class TranscodingHelperService {

    private static final Logger log = LoggerFactory.getLogger(TranscodingHelperService.class);

    private final S3Service s3Service;
    private final VideoRepository videoRepository;
    private final VideoFileRepository videoFileRepository;
    private final TranscodingJobRepository jobRepository;
    private final FFmpegHelper ffmpegHelper;
    private final VideoServiceHelper videoServiceHelper;

    public TranscodingHelperService(
            S3Service s3Service,
            VideoRepository videoRepository, VideoFileRepository videoFileRepository,
            TranscodingJobRepository jobRepository,
            FFmpegHelper ffmpegHelper,
            VideoServiceHelper videoServiceHelper
    ) {
        this.s3Service = s3Service;
        this.videoRepository = videoRepository;
        this.videoFileRepository = videoFileRepository;
        this.jobRepository = jobRepository;
        this.ffmpegHelper = ffmpegHelper;
        this.videoServiceHelper = videoServiceHelper;
    }

    public Path downloadOriginalVideo(UUID videoId, String key) {
        try {
            return s3Service.downloadOriginalVideo(videoId, key);
        } catch (IOException e) {
            throw new RuntimeException("Failed to download original video " + videoId, e);
        }
    }

    public TranscodingJob queueTranscodingJob(UUID videoId,
                                              EncodeFormat format,
                                              Resolution resolution) {

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        TranscodingJob job = TranscodingJob.builder()
                .video(video)
                .targetEncoding(format)
                .targetResolution(resolution)
                .status(JobStatus.QUEUED)
                .priority(format.getPriority())
                .workerId(UUID.randomUUID().toString())
                .queuedAt(Instant.now())
                .build();

        return jobRepository.save(job);
    }

    public void processTranscodingJob(TranscodingJob job, VideoMetadata metadata, Path inputFile) {
        log.info("Processing transcoding job {}", job.getId());
        markProcessing(job);
        Path outputFile = null;

        try {
            EncodeFormat format = job.getTargetEncoding();
            Resolution resolution = job.getTargetResolution();
            outputFile = ffmpegHelper.runFfmpeg(inputFile, format, resolution, metadata);

            if (outputFile != null) {
                UploadResult uploadResult = videoServiceHelper.uploadTranscodedVideoAsync(
                        outputFile.toFile(),
                        buildOutputS3Key(job)
                );
                saveVideoFile(job, uploadResult, outputFile, format, resolution);
                markCompleted(job);
            }

        } catch (Exception e) {
            log.error("Transcoding job {} failed", job.getId(), e);
            markFailed(job, e.getMessage());
        } finally {
            deleteSilently(outputFile);
        }
    }

    private void markProcessing(TranscodingJob job) {
        log.info("Marking job {} as processing", job.getId());
        job.setStatus(JobStatus.PROCESSING);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);
    }

    private void markCompleted(TranscodingJob job) {
        log.info("Marking job {} as completed", job.getId());
        job.setStatus(JobStatus.COMPLETED);
        job.setCompletedAt(Instant.now());
        jobRepository.save(job);
    }

    private void markFailed(TranscodingJob job, String message) {
        log.error("Marking job {} as failed: {}", job.getId(), message);
        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage(message);
        job.setCompletedAt(Instant.now());
        jobRepository.save(job);
    }

    private String buildOutputS3Key(TranscodingJob job) {
        return String.format(
                "%s/%s/%s.mp4",
                job.getVideo().getId(),
                job.getTargetEncoding(),
                job.getTargetResolution()
        );
    }

    private void saveVideoFile(
            TranscodingJob job, UploadResult uploadResult, Path outputFile,
            EncodeFormat format, Resolution resolution
    ) throws IOException {

        Video video = job.getVideo();

        VideoFile videoFile = VideoFile.builder()
                .video(video)
                .encoding(format)
                .resolution(Resolution.valueOf(resolution.name()))
                .width(resolution.getWidth())
                .height(resolution.getHeight())
                .bitrate(100) // TODO: Retrieve bitrate
                .fps(30)
                .fileSizeBytes(Files.size(outputFile))
                .fileKey(uploadResult.key())
                .primary(resolution == Resolution.R1080P)
                .build();

        videoFileRepository.save(videoFile);

        log.info(
                "Saved transcoded video file {} {} for video {}",
                format,
                resolution,
                video.getId()
        );
    }

    public void deleteSilently(Path path) {
        if (path == null) return;

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temp file {}", path, e);
        }
    }
}