package dev.ograh.videostreaming.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "video_views", indexes = {
        @Index(name = "idx_video_view_video_id", columnList = "video_id"),
        @Index(name = "idx_video_view_user_id", columnList = "user_id"),
        @Index(name = "idx_video_view_viewed_at", columnList = "viewed_at")
})
public class VideoView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionId;
    private String ipAddress;
    private String userAgent;
    private String countryCode;
    private long watchSecs = 0;
    private boolean completed = false;
    private Instant viewedAt = Instant.now();

    @ManyToOne
    @JoinColumn(name = "video_id", referencedColumnName = "id")
    private Video video;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

}