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
import dev.ograh.videostreaming.enums.VideoStatus;
import dev.ograh.videostreaming.exception.InvalidVideoException;
import dev.ograh.videostreaming.service.S3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Component
public class VideoServiceHelper {

    private final S3Service s3Service;

    private static final Logger log = LoggerFactory.getLogger(VideoServiceHelper.class);

    public VideoServiceHelper(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    public void validateVideoFile(MultipartFile file) {
        if (file.isEmpty() || file.getContentType() == null) {
            throw new InvalidVideoException("Video file cannot be empty.");
        }

        if (!file.getContentType().startsWith("video/")) {
            throw new InvalidVideoException("Invalid video file type.");
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

}