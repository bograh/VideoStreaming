package dev.ograh.videostreaming.controller;

import dev.ograh.videostreaming.dto.request.VideoUploadRequest;
import dev.ograh.videostreaming.dto.response.VideoUploadResponse;
import dev.ograh.videostreaming.dto.shared.ApiResponse;
import dev.ograh.videostreaming.service.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;

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

}