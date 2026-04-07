package dev.ograh.videostreaming.utils;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
public class VideoCacheService {

    @CacheEvict(value = "videos", key = "#videoId")
    public void evictVideo(String videoId) {
    }
}