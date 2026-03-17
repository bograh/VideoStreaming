package dev.ograh.videostreaming.entity;

import dev.ograh.videostreaming.enums.VideoStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "videos", indexes = {
        @Index(name = "idx_video_title", columnList = "title", unique = true),
        @Index(name = "idx_video_status", columnList = "status"),
        @Index(name = "idx_video_user_id", columnList = "user_id"),
        @Index(name = "idx_videos_created_at", columnList = "created_at")
})
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, name = "duration_secs")
    private long durationSecs;

    @Column(nullable = false, name = "thumbnail_url")
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    private VideoStatus status;

    @ElementCollection
    @CollectionTable(name = "video_tags", joinColumns = @JoinColumn(name = "video_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private long viewCount = 0;

    @Builder.Default
    @Column(name = "like_count", nullable = false)
    private long likeCount = 0;

    @Builder.Default
    @Column(name = "dislike_count", nullable = false)
    private long dislikeCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Builder.Default
    @OneToMany(mappedBy = "video", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VideoFile> videoFiles = new ArrayList<>();

    public void addVideoFile(VideoFile videoFile) {
        videoFiles.add(videoFile);
        videoFile.setVideo(this);
    }
}