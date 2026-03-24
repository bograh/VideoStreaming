package dev.ograh.videostreaming.utils;

import dev.ograh.videostreaming.dto.projection.VideoFileProjection;
import dev.ograh.videostreaming.dto.projection.VideoProjection;
import dev.ograh.videostreaming.dto.projection.VideoSummaryProjection;
import dev.ograh.videostreaming.dto.response.VideoResolutionsUrl;
import dev.ograh.videostreaming.dto.response.VideoResponse;
import dev.ograh.videostreaming.dto.response.VideoSummaryResponse;
import dev.ograh.videostreaming.repository.VideoFileRepository;
import dev.ograh.videostreaming.repository.VideoRepository;
import dev.ograh.videostreaming.service.S3Service;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class VideoResponseBuilder {

    private final S3Service s3Service;
    private final VideoRepository videoRepository;
    private final VideoFileRepository videoFileRepository;

    public VideoResponseBuilder(
            S3Service s3Service,
            VideoRepository videoRepository,
            VideoFileRepository videoFileRepository
    ) {
        this.s3Service = s3Service;
        this.videoRepository = videoRepository;
        this.videoFileRepository = videoFileRepository;
    }

    public VideoSummaryResponse buildSummaryResponse(
            VideoSummaryProjection video, Map<UUID, List<String>> tagsMap
    ) {
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

    public VideoResponse buildVideoResponse(VideoProjection video) {
        UUID videoId = video.getId();
        List<VideoFileProjection> files = videoFileRepository.findProjectedFilesByVideoId(videoId);
        List<String> tags = videoRepository.findTagsByVideoId(videoId);
        String thumbnailUrl = s3Service.getPresignedUrl(video.getThumbnailKey());

        List<VideoResolutionsUrl> resolutions = files.stream()
                .collect(Collectors.toMap(
                        f -> f.getResolution().getLabel(),
                        f -> new VideoResolutionsUrl(
                                f.getResolution().getLabel(),
                                s3Service.getPresignedUrl(f.getFileKey())),
                        (existing, replacement) -> existing))
                .values()
                .stream()
                .toList();

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