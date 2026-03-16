package dev.ograh.videostreaming.entity;

import dev.ograh.videostreaming.enums.VideoStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
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

    @Column(nullable = false)
    private long durationSecs;

    @Column(nullable = false)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    private VideoStatus status;

    @ElementCollection
    @CollectionTable(name = "video_tags", joinColumns = @JoinColumn(name = "video_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    private long viewCount;
    private long likeCount;
    private long dislikeCount;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();


    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

}