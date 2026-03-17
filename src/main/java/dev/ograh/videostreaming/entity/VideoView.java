package dev.ograh.videostreaming.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
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

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "user_agent", nullable = false)
    private String userAgent;

    @Column(name = "country_code", nullable = false)
    private String countryCode;

    @Builder.Default
    @Column(name = "watch_secs", nullable = false)
    private long watchSecs = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "viewed_at", nullable = false)
    private Instant viewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", referencedColumnName = "id")
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

}