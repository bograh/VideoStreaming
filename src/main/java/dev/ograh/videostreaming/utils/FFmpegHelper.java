package dev.ograh.videostreaming.utils;

import dev.ograh.videostreaming.dto.shared.VideoMetadata;
import dev.ograh.videostreaming.enums.EncodeFormat;
import dev.ograh.videostreaming.enums.VideoResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class FFmpegHelper {

    private static final Logger log = LoggerFactory.getLogger(FFmpegHelper.class);

    public Path runFfmpeg(Path inputFile, EncodeFormat format, VideoResolution resolution, VideoMetadata metadata)
            throws IOException, InterruptedException {

        if (resolution.getHeight() > metadata.dimension().height()) {
            log.info("Skipping {} because source resolution is {}p", resolution, metadata.dimension().height());
            return null;
        }

        Path outputFile = Files.createTempFile(
                inputFile.getParent(), "transcode_", "_" + format + "_" + resolution + ".mp4");

        List<String> cmd = buildFfmpegCommand(inputFile, outputFile, format, resolution);

        log.info("Executing FFmpeg: {}", String.join(" ", cmd));

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

        boolean finished = process.waitFor(20, TimeUnit.MINUTES);
        int exitCode = finished ? process.exitValue() : -1;

        if (!finished || exitCode != 0) {
            process.destroyForcibly();
            log.error("FFmpeg failed (exitCode={}):", exitCode);
            outputLines.forEach(line -> log.error("[ffmpeg] {}", line));
            throw new RuntimeException(
                    String.format("FFmpeg failed for format=%s resolution=%s exitCode=%d",
                            format, resolution, exitCode));
        }

        return outputFile;
    }

    private List<String> buildFfmpegCommand(Path inputFile, Path outputFile,
                                            EncodeFormat format, VideoResolution resolution) {

        assert format.getFfmpegCodec() != null;
        List<String> cmd = new ArrayList<>(List.of(
                "ffmpeg",
                "-i", inputFile.toAbsolutePath().toString(),
                "-vf", resolution.toScaleFilter(),
                "-c:v", format.getFfmpegCodec(),
                "-c:a", "aac",
                "-movflags", "+faststart",
                "-threads", String.valueOf(Runtime.getRuntime().availableProcessors() / 2)
        ));

        switch (format) {
            case H264 -> cmd.addAll(List.of("-preset", "fast", "-crf", "23"));
            case H265 -> cmd.addAll(List.of("-preset", "fast", "-crf", "28"));
            case AV1 -> cmd.addAll(List.of("-cpu-used", "4", "-crf", "32", "-b:v", "0"));
            case MPEG4 -> cmd.addAll(List.of("-q:v", "5"));
        }

        cmd.add("-y");
        cmd.add(outputFile.toAbsolutePath().toString());

        return cmd;
    }
}