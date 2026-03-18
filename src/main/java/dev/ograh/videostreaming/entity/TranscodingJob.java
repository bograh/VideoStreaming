package dev.ograh.videostreaming.entity;

import dev.ograh.videostreaming.enums.EncodeFormat;
import dev.ograh.videostreaming.enums.JobStatus;
import dev.ograh.videostreaming.enums.Resolution;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
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

    @Enumerated(EnumType.STRING)
    private EncodeFormat targetEncoding;

    @Enumerated(EnumType.STRING)
    private Resolution targetResolution;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(nullable = false)
    @Min(1)
    @Max(10)
    private int priority;

    @Builder.Default
    @Column(nullable = false)
    private int attempts = 0;

    @Builder.Default
    @Column(nullable = false, name = "max_attempts")
    private int maxAttempts = 3;

    @Column(name = "worker_id")
    private String workerId;


    @Column(name = "ffmpeg_cmd", columnDefinition = "TEXT")
    private String ffmpegCmd;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false, name = "progress_percent")
    @Min(0)
    @Max(100)
    private int progressPercent;

    @CreationTimestamp
    @Column(name = "queued_at", nullable = false)
    private Instant queuedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", referencedColumnName = "id")
    private Video video;
}