package dev.ograh.videostreaming.controller;

import dev.ograh.videostreaming.dto.request.CommentRequest;
import dev.ograh.videostreaming.dto.response.CommentResponse;
import dev.ograh.videostreaming.dto.shared.ApiResponse;
import dev.ograh.videostreaming.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/save/{videoId}")
    public ResponseEntity<ApiResponse<CommentResponse>> saveComment(
            @PathVariable String videoId, @RequestBody CommentRequest commentRequest, HttpServletRequest request
    ) {
        CommentResponse commentResponse = commentService.saveComment(videoId, commentRequest, request);
        ApiResponse<CommentResponse> apiResponse = ApiResponse.success(
                "Comment saved successfully", commentResponse);
        return ResponseEntity.ok(apiResponse);
    }
}