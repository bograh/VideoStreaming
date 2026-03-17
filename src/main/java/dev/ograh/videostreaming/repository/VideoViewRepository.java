package dev.ograh.videostreaming.repository;

import dev.ograh.videostreaming.entity.VideoView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoViewRepository extends JpaRepository<VideoView, Long> {
}