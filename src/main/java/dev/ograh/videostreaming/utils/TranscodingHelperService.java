package dev.ograh.videostreaming.utils;

import dev.ograh.videostreaming.dto.shared.UploadResult;
import dev.ograh.videostreaming.dto.shared.VideoMetadata;
import dev.ograh.videostreaming.entity.TranscodingJob;
import dev.ograh.videostreaming.entity.Video;
import dev.ograh.videostreaming.entity.VideoFile;
import dev.ograh.videostreaming.enums.EncodeFormat;
import dev.ograh.videostreaming.enums.JobStatus;
import dev.ograh.videostreaming.enums.Resolution;
import dev.ograh.videostreaming.enums.VideoStatus;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class TranscodingHelperService {

    private static final Logger log = LoggerFactory.getLogger(TranscodingHelperService.class);

    private static final int PLACEHOLDER_BITRATE = 0;
    private static final int PLACEHOLDER_FPS = 30;

    private final S3Service s3Service;
    private final VideoRepository videoRepository;
    private final VideoFileRepository videoFileRepository;
    private final TranscodingJobRepository jobRepository;
    private final FFmpegHelper ffmpegHelper;
    private final VideoServiceHelper videoServiceHelper;

    public TranscodingHelperService(
            S3Service s3Service,
            VideoRepository videoRepository,
            VideoFileRepository videoFileRepository,
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

    public TranscodingJob queueTranscodingJob(UUID videoId, EncodeFormat format, Resolution resolution) {
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
        Path hlsDir = null;

        try {
            Resolution resolution = job.getTargetResolution();
            EncodeFormat format = job.getTargetEncoding();
            hlsDir = ffmpegHelper.runFfmpegHls(inputFile, format, resolution, metadata);

            if (hlsDir != null) {
                String baseKey = "videos/" + job.getVideo().getId() + "/" + resolution;
                List<UploadResult> uploads = videoServiceHelper.uploadHlsDirectory(hlsDir, baseKey);

                saveHlsPlaylistFile(job, uploads, resolution);
                markCompleted(job);

                UUID videoId = job.getVideo().getId();
                if (allJobsCompleted(videoId)) {
                    generateAndUploadMasterPlaylist(videoId);
                }
            } else {
                // If the Source resolution is lower than the target, nothing to transcode.
                log.info("Job {} skipped — source resolution too low for {}", job.getId(), job.getTargetResolution());
                markCompleted(job);
            }
        } catch (Exception e) {
            log.error("Transcoding job {} failed", job.getId(), e);
            markFailed(job, e.getMessage());
        } finally {
            deleteDirectorySilently(hlsDir);
        }
    }

    public List<EncodeFormat> getTargetFormats() {
        return List.of(EncodeFormat.H264, EncodeFormat.H265);
    }

    public List<Resolution> getTargetResolutions(VideoMetadata metadata) {
        return Stream.of(Resolution.R1080P, Resolution.R720P, Resolution.R480P)
                .filter(r -> r.getHeight() <= metadata.dimension().height())
                .toList();
    }

    public boolean shouldProcess(EncodeFormat format, Resolution resolution) {
        return switch (format) {
            case H264 -> true;
            case H265 -> resolution == Resolution.R1080P;
            default -> false;
        };
    }

    public boolean allJobsCompleted(UUID videoId) {
        return jobRepository.countByVideoIdAndStatusNotIn(
                videoId, List.of(JobStatus.COMPLETED, JobStatus.FAILED)) == 0;
    }

    public void generateAndUploadMasterPlaylist(UUID videoId) {
        List<VideoFile> playlists = videoFileRepository.findByVideoId(videoId).stream()
                .filter(f -> f.getFileKey().endsWith("index.m3u8"))
                .toList();

        if (playlists.isEmpty()) {
            log.warn("No HLS playlists found for video {}", videoId);
            return;
        }

        String masterPlaylist = buildMasterPlaylist(playlists);
        String key = "videos/" + videoId + "/master.m3u8";
        s3Service.uploadString(key, masterPlaylist, "application/vnd.apple.mpegurl").join();
        log.info("Master playlist uploaded for video {}", videoId);

        videoRepository.findById(videoId).ifPresent(video -> {
            video.setStatus(VideoStatus.COMPLETED);
            videoRepository.save(video);
            log.info("Video {} marked as COMPLETED", videoId);
        });
    }

    private void markProcessing(TranscodingJob job) {
        job.setStatus(JobStatus.PROCESSING);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);
        log.info("Job {}  PROCESSING", job.getId());
    }

    private void markCompleted(TranscodingJob job) {
        job.setStatus(JobStatus.COMPLETED);
        job.setCompletedAt(Instant.now());
        jobRepository.save(job);
        log.info("Job {}  COMPLETED", job.getId());
    }

    private void markFailed(TranscodingJob job, String message) {
        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage(message);
        job.setCompletedAt(Instant.now());
        jobRepository.save(job);
        log.error("Job {}  FAILED: {}", job.getId(), message);
    }

    private void saveHlsPlaylistFile(TranscodingJob job, List<UploadResult> uploads, Resolution resolution) {
        UploadResult playlist = uploads.stream()
                .filter(u -> u.key().endsWith("index.m3u8"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("HLS upload missing index.m3u8"));

        VideoFile videoFile = VideoFile.builder()
                .video(job.getVideo())
                .fileKey(playlist.key())
                .encoding(job.getTargetEncoding())
                .resolution(resolution)
                .width(resolution.getWidth())
                .height(resolution.getHeight())
                .bitrate(PLACEHOLDER_BITRATE)
                .fileSizeBytes(0L)
                .fps(PLACEHOLDER_FPS)
                .build();

        videoFileRepository.save(videoFile);
    }

    private String buildMasterPlaylist(List<VideoFile> playlists) {
        StringBuilder sb = new StringBuilder("#EXTM3U\n");

        for (VideoFile file : playlists) {
            Resolution res = file.getResolution();
            sb.append("#EXT-X-STREAM-INF:BANDWIDTH=")
                    .append(bitrateFor(res))
                    .append(",RESOLUTION=")
                    .append(res.getWidth()).append("x").append(res.getHeight())
                    .append("\n")
                    .append("/api/videos/playlist?key=")
                    .append(file.getFileKey())
                    .append("\n");
        }

        return sb.toString();
    }

    private int bitrateFor(Resolution res) {
        return switch (res) {
            case R1080P -> 5_000_000;
            case R720P -> 2_800_000;
            case R480P -> 1_400_000;
            case R360P -> 800_000;
            case R240P -> 400_000;
            default -> 1_000_000;
        };
    }

    public void deleteSilently(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temp file {}", path, e);
        }
    }

    public void deleteDirectorySilently(Path dir) {
        if (dir == null) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }
}