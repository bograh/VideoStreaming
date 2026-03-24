package dev.ograh.videostreaming.repository;

import dev.ograh.videostreaming.entity.TranscodingJob;
import dev.ograh.videostreaming.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TranscodingJobRepository extends JpaRepository<TranscodingJob, UUID> {
    int countByVideoIdAndStatusNotIn(UUID videoId, List<JobStatus> completed);
}