package dev.ograh.videostreaming.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class CacheConfig {

    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(List.of("videos", "users"));

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(15))
                .maximumSize(1000)
        );

        cacheManager.registerCustomCache("videos", Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30))
                .maximumSize(500)
                .build()
        );

        cacheManager.registerCustomCache("users", Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(60))
                .maximumSize(1000)
                .build()
        );

        return cacheManager;
    }

}