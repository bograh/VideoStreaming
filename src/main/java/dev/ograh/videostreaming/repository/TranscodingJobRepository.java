package dev.ograh.videostreaming.repository;

import dev.ograh.videostreaming.entity.TranscodingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TranscodingJobRepository extends JpaRepository<TranscodingJob, UUID> {
}