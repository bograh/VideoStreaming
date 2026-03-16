package dev.ograh.videostreaming.entity;

import dev.ograh.videostreaming.enums.JobStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transcoding_jobs", indexes = {
        @Index(name = "idx_transcoding_jobs_video_id", columnList = "video_id"),
        @Index(name = "idx_transcoding_jobs_queued", columnList = "queued_at"),
        @Index(name = "idx_transcoding_jobs_status", columnList = "status"),
})
public class TranscodingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String targetEncoding;

    @Column(nullable = false)
    private String targetResolution;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(nullable = false)
    @Min(1)
    @Max(10)
    private int priority;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private int maxAttempts = 3;

    private String workerId;

    @Column(columnDefinition = "TEXT")
    private String ffmpegCmd;

    private String errorMessage;

    @Column(nullable = false)
    @Min(0)
    @Max(100)
    private int progressPercent;

    private Instant queuedAt = Instant.now();
    private Instant startedAt;
    private Instant completedAt;

    @ManyToOne
    @JoinColumn(name = "video_id", referencedColumnName = "id")
    private Video video;
}