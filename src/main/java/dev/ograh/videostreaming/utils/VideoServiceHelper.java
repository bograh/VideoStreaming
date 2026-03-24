package dev.ograh.videostreaming.utils;

import dev.ograh.videostreaming.dto.request.VideoUploadRequest;
import dev.ograh.videostreaming.dto.response.VideoUploadResponse;
import dev.ograh.videostreaming.dto.shared.UploadResult;
import dev.ograh.videostreaming.dto.shared.VideoDimension;
import dev.ograh.videostreaming.dto.shared.VideoMetadata;
import dev.ograh.videostreaming.entity.User;
import dev.ograh.videostreaming.entity.Video;
import dev.ograh.videostreaming.entity.VideoFile;
import dev.ograh.videostreaming.enums.EncodeFormat;
import dev.ograh.videostreaming.enums.VideoSortField;
import dev.ograh.videostreaming.enums.VideoStatus;
import dev.ograh.videostreaming.exception.InvalidVideoException;
import dev.ograh.videostreaming.repository.VideoRepository;
import dev.ograh.videostreaming.service.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class VideoServiceHelper {

    private static final Logger log = LoggerFactory.getLogger(VideoServiceHelper.class);

    private final S3Service s3Service;
    private final VideoRepository videoRepository;

    public VideoServiceHelper(S3Service s3Service, VideoRepository videoRepository) {
        this.s3Service = s3Service;
        this.videoRepository = videoRepository;
    }

    public void validateVideoFile(MultipartFile file, String title) {
        if (file.isEmpty() || file.getContentType() == null) {
            throw new InvalidVideoException("Video file cannot be empty.");
        }
        if (!file.getContentType().startsWith("video/")) {
            throw new InvalidVideoException("Invalid video file type.");
        }
        if (videoRepository.existsByTitleLikeIgnoreCase(title)) {
            throw new InvalidVideoException("Video with title '" + title + "' already exists.");
        }
    }

    public File createTempFile(MultipartFile file, String prefix) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String safeSuffix = (originalFilename != null)
                ? "_" + Paths.get(originalFilename).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_")
                : ".tmp";
        File tempFile = File.createTempFile(prefix, safeSuffix);
        file.transferTo(tempFile);
        return tempFile;
    }

    public void deleteTempFile(File file) {
        if (file != null && file.exists() && !file.delete()) {
            log.error("Failed to delete temp file: {}", file.getAbsolutePath());
        }
    }

    public UploadResult uploadVideoAsync(File file) {
        return s3Service.uploadVideo(file).join();
    }

    public UploadResult uploadTranscodedVideoAsync(File file, String s3Key) {
        return s3Service.uploadTranscodedFile(file, s3Key).join();
    }

    public UploadResult uploadThumbnailAsync(File file) {
        return s3Service.uploadThumbnail(file).join();
    }

    public List<UploadResult> uploadHlsDirectory(Path dir, String baseKey) throws IOException {
        List<CompletableFuture<UploadResult>> futures = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(dir)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                String relative = dir.relativize(path).toString().replace("\\", "/");
                String s3Key = baseKey + "/" + relative;
                futures.add(s3Service.uploadTranscodedFile(path.toFile(), s3Key));
            });
        }

        return futures.stream().map(CompletableFuture::join).toList();
    }

    public Video buildVideoEntity(
            VideoUploadRequest request, User user, VideoMetadata metadata, UploadResult thumbnailUpload
    ) {
        return Video.builder()
                .title(request.title())
                .description(request.description())
                .tags(request.tags())
                .durationSecs(metadata.durationSecs())
                .thumbnailKey(thumbnailUpload.key())
                .user(user)
                .status(VideoStatus.PENDING)
                .build();
    }

    public VideoFile buildVideoFileEntity(
            UploadResult videoUpload, VideoMetadata metadata, long fileSize, Video video
    ) {
        VideoDimension dimension = metadata.dimension();
        return VideoFile.builder()
                .fileKey(videoUpload.key())
                .fps(metadata.fps())
                .bitrate(metadata.bitrate())
                .encoding(EncodeFormat.ORIGINAL)
                .primary(true)
                .resolution(metadata.resolution())
                .height(dimension.height())
                .width(dimension.width())
                .fileSizeBytes(fileSize)
                .video(video)
                .build();
    }

    public VideoUploadResponse buildUploadResponse(
            Video video, UploadResult videoUpload, UploadResult thumbnailUpload,
            List<String> tags, VideoMetadata metadata
    ) {
        return new VideoUploadResponse(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                thumbnailUpload.url(),
                videoUpload.url(),
                tags,
                metadata.durationSecs(),
                video.getStatus(),
                video.getCreatedAt()
        );
    }

    public void validatePlaylistKey(String key) {
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing playlist key");
        }
        if (!key.startsWith("videos/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid playlist key");
        }
        if (!key.endsWith(".m3u8")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Key must refer to an m3u8 file");
        }
        if (key.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid playlist key");
        }
    }

    public Pageable createPageable(int page, int size, String sortBy, String orderBy) {
        VideoSortField sortField = VideoSortField.from(sortBy);
        Sort.Direction direction = Sort.Direction.DESC;

        if (orderBy != null) {
            try {
                direction = Sort.Direction.fromString(orderBy);
            } catch (IllegalArgumentException ignored) {
            }
        }

        return PageRequest.of(page, size, Sort.by(direction, sortField.getField()));
    }

    @Cacheable(value = "videos", key = "'playlist:' + #playlistKey")
    public String buildRewrittenPlaylist(String playlistKey) {
        String rawPlaylist;
        try {
            rawPlaylist = s3Service.downloadString(playlistKey);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String keyPrefix = playlistKey.substring(0, playlistKey.lastIndexOf('/') + 1);

        String result = Arrays.stream(rawPlaylist.split("\\r?\\n"))
                .map(line -> {
                    String trimmed = line.trim();
                    if (!trimmed.startsWith("#") && trimmed.endsWith(".ts")) {
                        String segmentKey = keyPrefix + trimmed;
                        String presigned = s3Service.getPresignedUrl(segmentKey);
                        log.info("Rewriting segment: {} -> {}", segmentKey, presigned);
                        return presigned;
                    }
                    return line;
                })
                .collect(Collectors.joining("\n"));
        log.info("Rewritten playlist for {}:\n{}", playlistKey, result);
        return result;
    }

    private String buildPresignedPlaylistUrl(String playlistKey) {
        return "/api/videos/playlist?key=" + playlistKey;
    }
}