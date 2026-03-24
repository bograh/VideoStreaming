package dev.ograh.videostreaming.utils;

import dev.ograh.videostreaming.dto.shared.VideoMetadata;
import dev.ograh.videostreaming.enums.EncodeFormat;
import dev.ograh.videostreaming.enums.Resolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Component
public class FFmpegHelper {

    private static final Logger log = LoggerFactory.getLogger(FFmpegHelper.class);
    private static final int TIMEOUT_MINUTES = 20;

    public Path runFfmpeg(Path inputFile, EncodeFormat format, Resolution resolution, VideoMetadata metadata)
            throws IOException, InterruptedException {

        if (resolution == Resolution.AUDIO_ONLY) {
            log.info("Skipping {} — audio-only resolution", resolution);
            return null;
        }

        if (resolution.getHeight() > metadata.dimension().height()) {
            log.info("Skipping {} — source is only {}p", resolution, metadata.dimension().height());
            return null;
        }

        if (format.getFfmpegCodec() == null) {
            throw new IllegalArgumentException("No FFmpeg codec configured for format: " + format);
        }

        Path outputFile = Files.createTempFile(
                inputFile.getParent(), "transcode_", "_" + format + "_" + resolution + ".mp4");

        try {
            runProcess(buildFfmpegCommand(inputFile, outputFile, format, resolution), format + "/" + resolution);
        } catch (Exception e) {
            try {
                Files.deleteIfExists(outputFile);
            } catch (IOException ignored) {
            }
            throw e;
        }

        return outputFile;
    }

    public Path runFfmpegHls(Path inputFile, Resolution resolution, VideoMetadata metadata)
            throws IOException, InterruptedException {

        if (resolution.getHeight() > metadata.dimension().height()) {
            log.info("Skipping HLS {} — source is only {}p", resolution, metadata.dimension().height());
            return null;
        }

        Path outputDir = Files.createTempDirectory("hls_" + resolution);

        try {
            runProcess(buildHlsCommand(inputFile, outputDir, resolution), "HLS/" + resolution);
        } catch (Exception e) {
            // Clean up the temp directory on failure
            try (Stream<Path> walk = Files.walk(outputDir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
            } catch (IOException ignored) {
            }
            throw e;
        }

        return outputDir;
    }

    private void runProcess(List<String> cmd, String label) throws IOException, InterruptedException {
        log.info("Executing FFmpeg [{}]: {}", label, String.join(" ", cmd));

        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        List<String> outputLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputLines.add(line);
            }
        }

        boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);
        int exitCode = finished ? process.exitValue() : -1;

        if (!finished || exitCode != 0) {
            process.destroyForcibly();
            outputLines.forEach(line -> log.error("[ffmpeg] {}", line));
            throw new RuntimeException(
                    String.format("FFmpeg failed [%s] exitCode=%d", label, exitCode));
        }

        log.debug("FFmpeg [{}] completed successfully", label);
    }

    private List<String> buildFfmpegCommand(Path inputFile, Path outputFile,
                                            EncodeFormat format, Resolution resolution) {
        assert format.getFfmpegCodec() != null;
        List<String> cmd = new ArrayList<>(List.of(
                "ffmpeg",
                "-i", inputFile.toAbsolutePath().toString(),
                "-vf", resolution.toScaleFilter(),
                "-c:v", format.getFfmpegCodec(),
                "-c:a", "aac",
                "-movflags", "+faststart",
                "-threads", "1"
        ));

        switch (format) {
            case H264 -> cmd.addAll(List.of("-preset", "fast", "-crf", "23"));
            case H265 -> cmd.addAll(List.of("-preset", "fast", "-crf", "28"));
        }

        cmd.add("-y");
        cmd.add(outputFile.toAbsolutePath().toString());
        return cmd;
    }

    private List<String> buildHlsCommand(Path input, Path outputDir, Resolution resolution) {
        return List.of(
                "ffmpeg",
                "-i", input.toString(),
                "-vf", "scale=-2:" + resolution.getHeight(),
                "-c:a", "aac",
                "-ar", "48000",
                "-b:a", "128k",
                "-c:v", "libx264",
                "-profile:v", "main",
                "-crf", "20",
                "-sc_threshold", "0",
                "-g", "48",
                "-keyint_min", "48",
                "-hls_time", "6",
                "-hls_playlist_type", "vod",
                "-hls_segment_filename", outputDir.resolve("segment_%03d.ts").toString(),
                outputDir.resolve("index.m3u8").toString()
        );
    }
}