package dev.ograh.videostreaming.repository;

import dev.ograh.videostreaming.dto.projection.VideoFileProjection;
import dev.ograh.videostreaming.entity.VideoFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoFileRepository extends JpaRepository<VideoFile, UUID> {

    @Query("""
                SELECT
                    vf.resolution as resolution,
                    vf.fileKey as fileKey
                FROM VideoFile vf
                WHERE vf.video.id = :videoId
            """)
    List<VideoFileProjection> findProjectedFilesByVideoId(UUID videoId);
}