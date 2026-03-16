package dev.ograh.videostreaming.entity;

import dev.ograh.videostreaming.enums.EncodeFormat;
import dev.ograh.videostreaming.enums.Resolution;
import jakarta.persistence.*;
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
    private long fileSizeBytes;

    @Column(nullable = false)
    private String fileKey;

    @Column(nullable = false)
    private String fileUrl;

    private boolean isPrimary;

    private Instant createdAt;

    @ManyToOne
    @JoinColumn(name = "video_id", referencedColumnName = "id")
    private Video video;
}