package dev.ograh.videostreaming.utils;

import dev.ograh.videostreaming.dto.projection.VideoFileProjection;
import dev.ograh.videostreaming.dto.projection.VideoProjection;
import dev.ograh.videostreaming.dto.projection.VideoSummaryProjection;
import dev.ograh.videostreaming.dto.request.VideoUploadRequest;
import dev.ograh.videostreaming.dto.response.VideoResolutionsUrl;
import dev.ograh.videostreaming.dto.response.VideoResponse;
import dev.ograh.videostreaming.dto.response.VideoSummaryResponse;
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
import dev.ograh.videostreaming.repository.VideoFileRepository;
import dev.ograh.videostreaming.repository.VideoRepository;
import dev.ograh.videostreaming.service.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class VideoServiceHelper {

    private final S3Service s3Service;
    private final VideoRepository videoRepository;
    private final VideoFileRepository videoFileRepository;

    private static final Logger log = LoggerFactory.getLogger(VideoServiceHelper.class);

    public VideoServiceHelper(S3Service s3Service, VideoRepository videoRepository, VideoFileRepository videoFileRepository) {
        this.s3Service = s3Service;
        this.videoRepository = videoRepository;
        this.videoFileRepository = videoFileRepository;
    }

    public void validateVideoFile(MultipartFile file, String title) {
        if (file.isEmpty() || file.getContentType() == null) {
            throw new InvalidVideoException("Video file cannot be empty.");
        }

        if (!file.getContentType().startsWith("video/")) {
            throw new InvalidVideoException("Invalid video file type.");
        }

        if (videoRepository.existsByTitleLikeIgnoreCase(title)) {
            throw new InvalidVideoException("Video with title " + title + " already exists.");
        }
    }

    public File createTempFile(MultipartFile file, String prefix) throws IOException {
        String filename = file.getOriginalFilename();
        File tempFile = File.createTempFile(prefix, filename);
        file.transferTo(tempFile);
        return tempFile;
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

    public VideoUploadResponse buildResponse(
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

    public void deleteTempFile(File file) {
        if (file != null && file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                log.error("Failed to delete temp file: {}", file.getAbsolutePath());
            }
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

        Sort sort = Sort.by(direction, sortField.getField());
        return PageRequest.of(page, size, sort);
    }

    public VideoSummaryResponse buildVideoSummaryResponse(
            VideoSummaryProjection video, Map<UUID, List<String>> tagsMap) {
        String thumbnailUrl = s3Service.getPresignedUrl(video.getThumbnailKey());
        return new VideoSummaryResponse(
                String.valueOf(video.getId()),
                video.getTitle(),
                video.getUser(),
                video.getDurationSecs(),
                thumbnailUrl,
                video.getViewCount(),
                video.getCreatedAt().toString(),
                tagsMap.getOrDefault(video.getId(), Collections.emptyList())
        );
    }

    public VideoResponse buildVideoResponseFrom(VideoProjection video) {
        UUID videoId = video.getId();
        List<VideoFileProjection> files = videoFileRepository.findProjectedFilesByVideoId(videoId);
        List<String> tags = videoRepository.findTagsByVideoId(videoId);
        String thumbnailUrl = s3Service.getPresignedUrl(video.getThumbnailKey());
        List<VideoResolutionsUrl> resolutions = files.stream()
                .map(file -> new VideoResolutionsUrl(
                        file.getResolution().getLabel(),
                        s3Service.getPresignedUrl(file.getFileKey()))).toList();

        return new VideoResponse(
                String.valueOf(videoId),
                video.getTitle(),
                video.getDescription(),
                thumbnailUrl,
                resolutions,
                tags,
                video.getDurationSecs(),
                video.getStatus(),
                video.getViewCount(),
                video.getLikeCount(),
                video.getDislikeCount(),
                video.getCreatedAt().toString()
        );
    }

}