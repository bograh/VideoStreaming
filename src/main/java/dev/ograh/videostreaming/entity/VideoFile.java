package dev.ograh.videostreaming.entity;

import dev.ograh.videostreaming.enums.EncodeFormat;
import dev.ograh.videostreaming.enums.Resolution;
import jakarta.persistence.*;
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
@Table(name = "video_files", indexes = {
        @Index(name = "idx_video_file_video_id", columnList = "video_id")
})
public class VideoFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private EncodeFormat encoding;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Resolution resolution;

    private long width;
    private long height;
    private long bitrate;
    private int fps;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "file_key", nullable = false)
    private String fileKey;

    @Builder.Default
    @Column(nullable = false, name = "is_primary")
    private boolean primary = false;

    @CreationTimestamp
    @Column(nullable = false, name = "created_at", updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", referencedColumnName = "id")
    private Video video;
}