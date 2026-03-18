package dev.ograh.videostreaming.service;

import dev.ograh.videostreaming.config.S3Config;
import dev.ograh.videostreaming.dto.shared.UploadResult;
import dev.ograh.videostreaming.utils.TempFileManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3Service.class);
    private final S3AsyncClient s3AsyncClient;
    private final S3Client s3Client;
    private final S3Config s3Config;
    private final S3Presigner s3Presigner;
    private final TempFileManager tempFileManager;

    public CompletableFuture<UploadResult> uploadThumbnail(File thumbnailFile) {
        return uploadFile(thumbnailFile, "thumbnails/", "image/jpeg");
    }

    public CompletableFuture<UploadResult> uploadVideo(File videoFile) {
        return uploadFile(videoFile, "videos/", "video/mp4");
    }

    public CompletableFuture<UploadResult> uploadTranscodedFile(File videoFile, String s3Key) {
        String key = "transcoded_videos/" + s3Key;
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(key)
                .contentType("video/mp4")
                .build();

        return s3AsyncClient.putObject(putObjectRequest, AsyncRequestBody.fromFile(videoFile))
                .thenApply(resp -> new UploadResult(s3Key, getPresignedUrl(s3Key)));
    }

    private CompletableFuture<UploadResult> uploadFile(File file, String prefix, String contentType) {
        String key = prefix + UUID.randomUUID();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(key)
                .contentType(contentType)
                .build();

        return s3AsyncClient.putObject(putObjectRequest, AsyncRequestBody.fromFile(file))
                .thenApply(resp -> new UploadResult(key, getPresignedUrl(key)));
    }

    public String getPresignedUrl(String key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60))
                .getObjectRequest(req -> req
                        .bucket(s3Config.getBucketName())
                        .key(key)
                        .build())
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public Path downloadOriginalVideo(UUID videoId, String key) throws IOException {
        Path localFile = tempFileManager.createTempFile("original_", ".mp4");
        localFile.toFile().deleteOnExit();

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(key)
                .build();

        s3AsyncClient.getObject(request, AsyncResponseTransformer.toFile(localFile)).join();
        return localFile;
    }
}