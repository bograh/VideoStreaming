package dev.ograh.videostreaming.utils;

import dev.ograh.videostreaming.dto.projection.CommentSummaryProjection;
import dev.ograh.videostreaming.dto.response.CommentResponse;
import dev.ograh.videostreaming.enums.CommentSortField;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class CommentServiceHelper {

    public Pageable createPageable(int page, int size, String sortBy, String orderBy) {
        Sort.Direction sortDirection = Sort.Direction.DESC;
        CommentSortField commentSortField = CommentSortField.from(sortBy);

        if (orderBy != null) {
            try {
                sortDirection = Sort.Direction.fromString(orderBy);
            } catch (IllegalArgumentException ignored) {
            }
        }

        Sort sort = Sort.by(sortDirection, commentSortField.getField());
        return PageRequest.of(page, size, sort);
    }

    public CommentResponse buildCommentResponse(CommentSummaryProjection comment) {
        return new CommentResponse(
                comment.getId(), comment.getComment(), comment.getParentCommentId(),
                comment.getCreatedAt().toString(), comment.getAuthor()
        );
    }

}