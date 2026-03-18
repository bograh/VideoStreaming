package dev.ograh.videostreaming.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

@Component
public class TempFileManager {

    private static final Logger log = LoggerFactory.getLogger(TempFileManager.class);

    private final Path baseTempDir;
    private static final Duration FILE_TTL = Duration.ofHours(2);

    public TempFileManager() throws IOException {
        this.baseTempDir = Paths.get(System.getProperty("user.home"), "video_tmp");
        if (!Files.exists(baseTempDir)) {
            Files.createDirectories(baseTempDir);
        }
    }

    public Path createTempFile(String prefix, String suffix) throws IOException {
        Path tempFile = Files.createTempFile(baseTempDir, prefix, suffix);
        log.info("Created temp file: {}", tempFile.toAbsolutePath());
        return tempFile;
    }

    public void deleteFile(Path file) {
        if (file != null) {
            try {
                Files.deleteIfExists(file);
                log.info("Deleted temp file: {}", file.toAbsolutePath());
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", file, e);
            }
        }
    }

    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void cleanupTempFiles() {
        if (!Files.exists(baseTempDir)) {
            return;
        }
        log.info("Running temp file cleanup for {}", baseTempDir);
        try (Stream<Path> files = Files.list(baseTempDir)) {
            Instant now = Instant.now();
            files.forEach(file -> {
                try {
                    FileTime lastModified = Files.getLastModifiedTime(file);
                    Duration age = Duration.between(lastModified.toInstant(), now);

                    if (age.compareTo(FILE_TTL) > 0) {
                        Files.deleteIfExists(file);
                        log.info("Deleted stale temp file: {}", file);
                    }

                } catch (Exception e) {
                    log.warn("Failed to delete temp file {}", file, e);
                }
            });

        } catch (IOException e) {
            log.error("Temp cleanup failed", e);
        }
    }
}