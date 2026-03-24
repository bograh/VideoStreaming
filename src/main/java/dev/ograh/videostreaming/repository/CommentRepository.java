package dev.ograh.videostreaming.repository;

import dev.ograh.videostreaming.dto.projection.CommentSummaryProjection;
import dev.ograh.videostreaming.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
                SELECT c.id as id,
                       c.body as comment,
                       c.likeCount as likeCount,
                       c.pinned as pinned,
                       c.hidden as hidden,
                       c.createdAt as createdAt,
                       u.name as author,
                       pc.id as parentCommentId
                FROM Comment c
                JOIN c.user u
                LEFT JOIN c.parent pc
                WHERE c.video.id = :videoId
                  AND c.hidden = false
            """)
    Page<CommentSummaryProjection> findAllCommentSummaries(
            Pageable pageable, @Param("videoId") UUID videoId);
}