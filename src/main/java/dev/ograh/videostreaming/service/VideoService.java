package dev.ograh.videostreaming.service;

import dev.ograh.videostreaming.dto.request.VideoUploadRequest;
import dev.ograh.videostreaming.dto.response.VideoUploadResponse;
import dev.ograh.videostreaming.dto.shared.UploadResult;
import dev.ograh.videostreaming.dto.shared.VideoDimension;
import dev.ograh.videostreaming.dto.shared.VideoMetadata;
import dev.ograh.videostreaming.entity.User;
import dev.ograh.videostreaming.entity.Video;
import dev.ograh.videostreaming.entity.VideoFile;
import dev.ograh.videostreaming.enums.EncodeFormat;
import dev.ograh.videostreaming.enums.Resolution;
import dev.ograh.videostreaming.enums.UserRole;
import dev.ograh.videostreaming.enums.VideoStatus;
import dev.ograh.videostreaming.exception.ForbiddenException;
import dev.ograh.videostreaming.exception.InvalidVideoException;
import dev.ograh.videostreaming.exception.VideoUploadException;
import dev.ograh.videostreaming.repository.UserRepository;
import dev.ograh.videostreaming.repository.VideoRepository;
import dev.ograh.videostreaming.utils.UserUtils;
import dev.ograh.videostreaming.utils.VideoMetadataUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class VideoService {

    private static final Logger log = LoggerFactory.getLogger(VideoService.class);
    private final VideoRepository videoRepository;
    private final UserUtils userUtils;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final VideoMetadataUtil videoMetadataUtil;

    public VideoUploadResponse uploadVideo(MultipartFile file, MultipartFile thumbnail, VideoUploadRequest videoUploadRequest, HttpServletRequest request) {
        if (file.isEmpty() || file.getContentType() == null) {
            throw new InvalidVideoException("Video file cannot be empty.");
        }

        if (!file.getContentType().startsWith("video/")) {
            throw new InvalidVideoException("Invalid video file type. Only video files are allowed.");
        }

        String email = userUtils.getUserEmailFromAuthHeader(request);
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow(
                () -> new ForbiddenException("You are not authorized to upload a video.")
        );

        if (user.getRole() != UserRole.CREATOR) {
            user.setRole(UserRole.CREATOR);
            userRepository.save(user);
        }

        File tempVideoFile = null;
        File tempThumbnailFile = null;
        try {
            String originalFilename = file.getOriginalFilename();
            tempVideoFile = File.createTempFile("upload-", originalFilename);
            file.transferTo(tempVideoFile);

            String thumbnailFilename = thumbnail.getOriginalFilename();
            tempThumbnailFile = File.createTempFile("thumbnail-", thumbnailFilename);
            thumbnail.transferTo(tempThumbnailFile);

            VideoMetadata metadata = videoMetadataUtil.extractMetadata(tempVideoFile);

            int videoDuration = metadata.durationSecs();
            int fps = metadata.fps();
            int bitrate = metadata.bitrate();
            Resolution resolution = metadata.resolution();
            VideoDimension dimension = metadata.dimension();
            long fileSizeBytes = tempVideoFile.length();

            CompletableFuture<UploadResult> videoFuture = s3Service.uploadVideo(tempVideoFile);
            CompletableFuture<UploadResult> thumbnailFuture = s3Service.uploadThumbnail(tempThumbnailFile);

            CompletableFuture.allOf(videoFuture, thumbnailFuture).join();

            UploadResult videoUploadResult = videoFuture.join();
            UploadResult thumbnailUploadResult = thumbnailFuture.join();

            Video video = Video.builder()
                    .title(videoUploadRequest.title())
                    .description(videoUploadRequest.description())
                    .tags(videoUploadRequest.tags())
                    .durationSecs(videoDuration)
                    .thumbnailKey(thumbnailUploadResult.key())
                    .user(user)
                    .status(VideoStatus.PENDING)
                    .build();

            VideoFile videoFile = VideoFile.builder()
                    .fileKey(videoUploadResult.key())
                    .fps(fps)
                    .bitrate(bitrate)
                    .encoding(EncodeFormat.ORIGINAL)
                    .primary(true)
                    .resolution(resolution)
                    .height(dimension.height())
                    .width(dimension.width())
                    .fileSizeBytes(fileSizeBytes)
                    .video(video)
                    .build();

            video.addVideoFile(videoFile);
            Video savedVideo = videoRepository.save(video);

            return new VideoUploadResponse(
                    savedVideo.getId(),
                    savedVideo.getTitle(),
                    savedVideo.getDescription(),
                    thumbnailUploadResult.url(),
                    videoUploadResult.url(),
                    videoUploadRequest.tags(),
                    videoDuration,
                    savedVideo.getStatus(),
                    savedVideo.getCreatedAt()
            );

        } catch (IOException e) {
            throw new VideoUploadException("Failed to upload video: " + e.getMessage());
        } finally {
            if (tempVideoFile != null && tempVideoFile.exists()) {
                boolean tempFileDeleted = tempVideoFile.delete();
                if (!tempFileDeleted) {
                    log.error("Failed to delete temporary file: {}", tempVideoFile.getAbsolutePath());
                }
            }
            if (tempThumbnailFile != null && tempThumbnailFile.exists()) {
                boolean thumbnailDeleted = tempThumbnailFile.delete();
                if (!thumbnailDeleted) {
                    log.error("Failed to delete tempThumbnailFile file: {}", tempThumbnailFile.getAbsolutePath());
                }
            }
        }
    }
}