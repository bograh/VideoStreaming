package dev.ograh.videostreaming.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ograh.videostreaming.dto.shared.VideoDimension;
import dev.ograh.videostreaming.dto.shared.VideoMetadata;
import dev.ograh.videostreaming.enums.Resolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Component
public class VideoMetadataUtil {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(VideoMetadataUtil.class);
    private static final int FFPROBE_TIMEOUT_SECONDS = 30;
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private JsonNode probe(File videoFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "quiet",
                    "-print_format", "json",
                    "-show_format",
                    "-show_streams",
                    videoFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();

            Future<String> outputFuture = executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    StringBuilder json = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        json.append(line);
                    }
                    return json.toString();
                }
            });

            boolean finished = process.waitFor(FFPROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("ffprobe timed out after " + FFPROBE_TIMEOUT_SECONDS + " seconds");
            }

            String json = outputFuture.get(5, TimeUnit.SECONDS);
            log.debug("ffprobe output: {}", json);

            if (json.isBlank()) {
                throw new RuntimeException("ffprobe returned empty output — file may be corrupt or unreadable");
            }

            return mapper.readTree(json);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to probe video: {}", e.getMessage());
            throw new RuntimeException("Failed to probe video: " + e.getMessage(), e);
        }
    }

    private JsonNode getVideoStream(JsonNode root) {
        JsonNode streams = root.get("streams");
        if (streams == null || streams.isEmpty()) {
            throw new RuntimeException("No streams found in video file");
        }
        for (JsonNode stream : streams) {
            JsonNode codecType = stream.get("codec_type");
            if (codecType != null && "video".equals(codecType.asText())) {
                return stream;
            }
        }
        throw new RuntimeException("No video stream found");
    }

    private int extractBitrate(JsonNode format, JsonNode videoStream) {
        if (format.hasNonNull("bit_rate")) {
            return format.get("bit_rate").asInt();
        }
        if (videoStream.hasNonNull("bit_rate")) {
            log.debug("bit_rate not found at format level, falling back to video stream");
            return videoStream.get("bit_rate").asInt();
        }
        log.warn("bit_rate not found at format or stream level, defaulting to 0");
        return 0;
    }

    public VideoMetadata extractMetadata(File videoFile) {
        JsonNode root = probe(videoFile);
        JsonNode format = root.get("format");
        JsonNode videoStream = getVideoStream(root);

        if (format == null) {
            throw new RuntimeException("ffprobe output missing 'format' node");
        }

        int duration = (int) format.get("duration").asDouble();
        int bitrate = extractBitrate(format, videoStream);

        String fpsRaw = videoStream.get("r_frame_rate").asText();
        String[] fpsParts = fpsRaw.split("/");
        if (fpsParts.length != 2) {
            throw new RuntimeException("Unexpected r_frame_rate format: " + fpsRaw);
        }
        int fps = (int) Math.round(Double.parseDouble(fpsParts[0]) / Double.parseDouble(fpsParts[1]));

        int width = videoStream.get("width").asInt();
        int height = videoStream.get("height").asInt();

        Resolution resolutionEnum = Resolution.fromHeight(height);

        return new VideoMetadata(duration, fps, bitrate, resolutionEnum, new VideoDimension(height, width));
    }
}