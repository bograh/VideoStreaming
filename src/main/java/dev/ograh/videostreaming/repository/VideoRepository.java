package dev.ograh.videostreaming.repository;

import dev.ograh.videostreaming.dto.projection.VideoProjection;
import dev.ograh.videostreaming.dto.projection.VideoSummaryProjection;
import dev.ograh.videostreaming.dto.projection.VideoTagProjection;
import dev.ograh.videostreaming.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {

    boolean existsByTitleLikeIgnoreCase(String title);

    @Query("""
                SELECT v.id as id,
                       v.title as title,
                       u.name as user,
                       v.durationSecs as durationSecs,
                       v.thumbnailKey as thumbnailKey,
                       v.viewCount as viewCount,
                       v.createdAt as createdAt
                FROM Video v
                JOIN v.user u
                ORDER BY v.createdAt DESC
            """)
    Page<VideoSummaryProjection> findAllVideoSummaries(Pageable pageable);

    @Query("""
                SELECT
                    v.id as id,
                    v.title as title,
                    v.description as description,
                    v.thumbnailKey as thumbnailKey,
                    v.durationSecs as durationSecs,
                    v.status as status,
                    v.viewCount as viewCount,
                    v.likeCount as likeCount,
                    v.dislikeCount as dislikeCount,
                    v.createdAt as createdAt
                FROM Video v
                WHERE v.id = :videoId
            """)
    Optional<VideoProjection> findProjectedVideo(UUID videoId);

    @Query("""
                SELECT t as tag
                FROM Video v
                JOIN v.tags t
                WHERE v.id = :videoId
            """)
    List<String> findTagsByVideoId(UUID videoId);

    @Query("""
                SELECT v.id as videoId, t as tag
                FROM Video v
                JOIN v.tags t
                WHERE v.id IN :videoIds
            """)
    List<VideoTagProjection> findTagsByVideoIds(@Param("videoIds") List<UUID> videoIds);

    @Query("""
                SELECT v.id AS id,
                       v.title AS title,
                       u.name AS user,
                       v.durationSecs AS durationSecs,
                       v.thumbnailKey AS thumbnailKey,
                       v.viewCount AS viewCount,
                       v.createdAt AS createdAt
                FROM Video v
                JOIN v.user u
                WHERE u.id = :userId
                ORDER BY v.createdAt DESC
            """)
    Page<VideoSummaryProjection> findAllVideoSummariesByUser(
            Pageable pageable, @Param("userId") UUID userId);
}