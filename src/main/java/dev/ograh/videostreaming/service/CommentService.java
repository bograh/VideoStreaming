package dev.ograh.videostreaming.service;

import dev.ograh.videostreaming.dto.projection.CommentSummaryProjection;
import dev.ograh.videostreaming.dto.request.CommentRequest;
import dev.ograh.videostreaming.dto.response.CommentResponse;
import dev.ograh.videostreaming.dto.shared.PageResponse;
import dev.ograh.videostreaming.entity.Comment;
import dev.ograh.videostreaming.entity.User;
import dev.ograh.videostreaming.entity.Video;
import dev.ograh.videostreaming.exception.ResourceNotFoundException;
import dev.ograh.videostreaming.repository.CommentRepository;
import dev.ograh.videostreaming.repository.VideoRepository;
import dev.ograh.videostreaming.utils.CommentServiceHelper;
import dev.ograh.videostreaming.utils.UserHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;
    private final UserHelper userHelper;
    private final CommentServiceHelper commentServiceHelper;

    @CacheEvict(value = "comments", allEntries = true)
    public CommentResponse saveComment(String videoId, CommentRequest commentRequest, HttpServletRequest request) {
        UUID videoUuid = UUID.fromString(videoId);
        Video video = videoRepository.findById(videoUuid).orElseThrow(
                () -> new ResourceNotFoundException("Video not found with id: " + videoId)
        );
        User user = userHelper.getAuthenticatedUser(request);

        Comment comment;

        if (commentRequest.parentCommentId() == 0) {
            comment = Comment.builder()
                    .body(commentRequest.body())
                    .user(user)
                    .video(video)
                    .createdAt(Instant.now())
                    .build();

            commentRepository.save(comment);
            return new CommentResponse(
                    comment.getId(),
                    comment.getBody(),
                    0L,
                    comment.getCreatedAt().toString(),
                    user.getName()
            );
        }

        Comment parentComment = commentRepository.findById(commentRequest.parentCommentId()).orElseThrow(
                () -> new ResourceNotFoundException("Parent comment not found with id: " + commentRequest.parentCommentId())
        );

        comment = Comment.builder()
                .body(commentRequest.body())
                .user(user)
                .video(video)
                .createdAt(Instant.now())
                .parent(parentComment)
                .build();
        commentRepository.save(comment);
        return new CommentResponse(
                comment.getId(),
                comment.getBody(),
                parentComment.getId(),
                comment.getCreatedAt().toString(),
                user.getName()
        );

    }

    @Cacheable(
            value = "comments",
            key = "'video=' + #videoId + 'page=' + #page + ',size=' + #size + ',sort=' + #sort + ',order=' + #order"
    )
    public PageResponse<List<CommentResponse>> getVideoComments(
            String videoId, int page, int size, String sort, String order
    ) {
        UUID videoUuid = UUID.fromString(videoId);

        if (!videoRepository.existsById(videoUuid)) {
            throw new ResourceNotFoundException("Video not found with id: " + videoId);
        }

        size = Math.min(size, 50);
        Pageable pageable = commentServiceHelper.createPageable(page, size, sort, order);

        Page<CommentSummaryProjection> commentPage =
                commentRepository.findAllCommentSummaries(pageable, videoUuid);

        List<CommentResponse> comments = commentPage.getContent()
                .stream()
                .map(commentServiceHelper::buildCommentResponse)
                .toList();

        return new PageResponse<>(
                comments,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                commentPage.getTotalPages(),
                commentPage.getTotalElements(),
                commentPage.isFirst(),
                commentPage.isLast(),
                commentPage.getNumberOfElements(),
                pageable.getSort().toString()
        );
    }
}