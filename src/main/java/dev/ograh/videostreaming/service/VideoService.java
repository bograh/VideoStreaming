package dev.ograh.videostreaming.service;

import dev.ograh.videostreaming.dto.request.VideoUploadRequest;
import dev.ograh.videostreaming.dto.response.VideoUploadResponse;
import dev.ograh.videostreaming.dto.shared.UploadResult;
import dev.ograh.videostreaming.dto.shared.VideoMetadata;
import dev.ograh.videostreaming.entity.User;
import dev.ograh.videostreaming.entity.Video;
import dev.ograh.videostreaming.entity.VideoFile;
import dev.ograh.videostreaming.exception.VideoUploadException;
import dev.ograh.videostreaming.repository.VideoRepository;
import dev.ograh.videostreaming.utils.UserHelper;
import dev.ograh.videostreaming.utils.VideoMetadataUtil;
import dev.ograh.videostreaming.utils.VideoServiceHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserHelper userHelper;
    private final VideoMetadataUtil videoMetadataUtil;
    private final VideoServiceHelper videoServiceHelper;

    public VideoUploadResponse uploadVideo(MultipartFile file, MultipartFile thumbnail, VideoUploadRequest videoUploadRequest, HttpServletRequest request) {
        videoServiceHelper.validateVideoFile(file);
        User user = userHelper.getAuthenticatedUser(request);

        File tempVideoFile = null;
        File tempThumbnailFile = null;
        try {
            tempVideoFile = videoServiceHelper.createTempFile(file, "upload-");
            tempThumbnailFile = videoServiceHelper.createTempFile(thumbnail, "thumbnail-");

            VideoMetadata metadata = videoMetadataUtil.extractMetadata(tempVideoFile);

            UploadResult videoUploadResult = videoServiceHelper.uploadVideoAsync(tempVideoFile);
            UploadResult thumbnailUploadResult = videoServiceHelper.uploadThumbnailAsync(tempThumbnailFile);

            Video video = videoServiceHelper.buildVideoEntity(videoUploadRequest, user, metadata, thumbnailUploadResult);
            VideoFile videoFile = videoServiceHelper.buildVideoFileEntity(videoUploadResult, metadata, tempVideoFile.length(), video);

            video.addVideoFile(videoFile);
            Video savedVideo = videoRepository.save(video);

            return videoServiceHelper.buildResponse(
                    savedVideo,
                    videoUploadResult,
                    thumbnailUploadResult,
                    videoUploadRequest.tags(),
                    metadata
            );

        } catch (IOException e) {
            throw new VideoUploadException("Failed to upload video: " + e.getMessage());
        } finally {
            videoServiceHelper.deleteTempFile(tempVideoFile);
            videoServiceHelper.deleteTempFile(tempThumbnailFile);
        }
    }
}