package dev.ograh.videostreaming.service;

import dev.ograh.videostreaming.dto.projection.VideoProjection;
import dev.ograh.videostreaming.dto.projection.VideoSummaryProjection;
import dev.ograh.videostreaming.dto.projection.VideoTagProjection;
import dev.ograh.videostreaming.dto.request.VideoUploadRequest;
import dev.ograh.videostreaming.dto.response.VideoResponse;
import dev.ograh.videostreaming.dto.response.VideoSummaryResponse;
import dev.ograh.videostreaming.dto.response.VideoUploadResponse;
import dev.ograh.videostreaming.dto.shared.PageResponse;
import dev.ograh.videostreaming.dto.shared.UploadResult;
import dev.ograh.videostreaming.dto.shared.VideoMetadata;
import dev.ograh.videostreaming.entity.User;
import dev.ograh.videostreaming.entity.Video;
import dev.ograh.videostreaming.entity.VideoFile;
import dev.ograh.videostreaming.exception.ResourceNotFoundException;
import dev.ograh.videostreaming.exception.VideoUploadException;
import dev.ograh.videostreaming.repository.VideoFileRepository;
import dev.ograh.videostreaming.repository.VideoRepository;
import dev.ograh.videostreaming.utils.UserHelper;
import dev.ograh.videostreaming.utils.VideoMetadataUtil;
import dev.ograh.videostreaming.utils.VideoServiceHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserHelper userHelper;
    private final VideoMetadataUtil videoMetadataUtil;
    private final VideoServiceHelper videoServiceHelper;
    private final VideoFileRepository videoFileRepository;

    @CacheEvict(value = "videos", allEntries = true)
    public VideoUploadResponse uploadVideo(MultipartFile file, MultipartFile thumbnail, VideoUploadRequest videoUploadRequest, HttpServletRequest request) {
        videoServiceHelper.validateVideoFile(file, videoUploadRequest.title());
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

    @Cacheable(
            value = "videos",
            condition = "#search == null",
            key = "'page=' + #page + ',size=' + #size + ',sortBy=' + #sortBy + ',orderBy=' + #orderBy"
    )
    public PageResponse<List<VideoSummaryResponse>> getVideos(int page, int size, String sortBy, String orderBy, String search) {
        Pageable pageable = videoServiceHelper.createPageable(page, Math.min(size, 50), sortBy, orderBy);

        Page<VideoSummaryProjection> videoPage = videoRepository.findAllVideoSummaries(pageable);
        List<UUID> videoIds = videoPage.stream().map(VideoSummaryProjection::getId).toList();

        List<VideoTagProjection> tags = videoRepository.findTagsByVideoIds(videoIds);

        Map<UUID, List<String>> tagsMap = tags.stream().collect(
                Collectors.groupingBy(VideoTagProjection::getVideoId,
                        Collectors.mapping(VideoTagProjection::getTag, Collectors.toList())));

        List<VideoSummaryResponse> videos = videoPage.stream()
                .map(v -> videoServiceHelper.buildVideoSummaryResponse(v, tagsMap))
                .toList();

        return new PageResponse<>(
                videos,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                videoPage.getTotalPages(),
                videoPage.getTotalElements(),
                videoPage.isFirst(),
                videoPage.isLast(),
                videoPage.getNumberOfElements(),
                videoPage.getSort().toString()
        );
    }

    @Cacheable(value = "videos", key = "#videoId")
    public VideoResponse getVideo(String videoId) {
        UUID videoUuid = UUID.fromString(videoId);
        VideoProjection video = videoRepository.findProjectedVideo(videoUuid).orElseThrow(
                () -> new ResourceNotFoundException("Video not found with id: " + videoId)
        );

        return videoServiceHelper.buildVideoResponseFrom(video);
    }

}