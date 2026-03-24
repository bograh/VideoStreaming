package dev.ograh.videostreaming.controller;

import dev.ograh.videostreaming.dto.request.VideoUploadRequest;
import dev.ograh.videostreaming.dto.response.CommentResponse;
import dev.ograh.videostreaming.dto.response.VideoResponse;
import dev.ograh.videostreaming.dto.response.VideoSummaryResponse;
import dev.ograh.videostreaming.dto.response.VideoUploadResponse;
import dev.ograh.videostreaming.dto.shared.ApiResponse;
import dev.ograh.videostreaming.dto.shared.PageResponse;
import dev.ograh.videostreaming.service.CommentService;
import dev.ograh.videostreaming.service.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;
    private final CommentService commentService;

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VideoUploadResponse>> uploadVideo(
            @RequestParam("video") MultipartFile video,
            @RequestParam(required = false, name = "thumbnail") MultipartFile thumbnail,
            @RequestParam @NotBlank String title,
            @RequestParam @NotBlank String description,
            @RequestParam(required = false) List<String> tags,
            HttpServletRequest request
    ) {
        VideoUploadRequest uploadRequest = new VideoUploadRequest(title, description, tags);
        VideoUploadResponse response = videoService.uploadVideo(video, thumbnail, uploadRequest, request);
        ApiResponse<VideoUploadResponse> apiResponse = ApiResponse.success(
                "Video uploaded successfully", response
        );
        return ResponseEntity.accepted().body(apiResponse);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<List<VideoSummaryResponse>>>> getVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "16") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String order,
            @RequestParam(required = false) String search
    ) {

        PageResponse<List<VideoSummaryResponse>> videoSummaryResponse = videoService.getVideos(
                page, size, sort, order, search);
        ApiResponse<PageResponse<List<VideoSummaryResponse>>> apiResponse = ApiResponse.success(
                "Videos retrieved successfully", videoSummaryResponse);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<List<VideoSummaryResponse>>>> getMyVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "16") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String order,
            HttpServletRequest request
    ) {

        PageResponse<List<VideoSummaryResponse>> videoSummaryResponse = videoService.getMyVideos(
                page, size, sort, order, request);
        ApiResponse<PageResponse<List<VideoSummaryResponse>>> apiResponse = ApiResponse.success(
                "Videos retrieved successfully", videoSummaryResponse);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<ApiResponse<VideoResponse>> getVideo(@PathVariable String videoId) {
        VideoResponse videoResponse = videoService.getVideo(videoId);
        ApiResponse<VideoResponse> apiResponse = ApiResponse.success(
                "Video retrieved successfully", videoResponse);

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{videoId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<List<CommentResponse>>>> getVideoComments(
            @PathVariable String videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "16") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String order
    ) {
        PageResponse<List<CommentResponse>> videoComments = commentService.getVideoComments(
                videoId, page, size, sort, order);
        ApiResponse<PageResponse<List<CommentResponse>>> apiResponse = ApiResponse.success(
                "Video comments retrieved successfully", videoComments);
        return ResponseEntity.ok(apiResponse);

    }

}