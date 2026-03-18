package dev.ograh.videostreaming.service;

import dev.ograh.videostreaming.config.S3Config;
import dev.ograh.videostreaming.dto.shared.UploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.File;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3AsyncClient s3Client;
    private final S3Config s3Config;
    private final S3Presigner s3Presigner;

    public CompletableFuture<UploadResult> uploadThumbnail(File thumbnailFile) {
        return uploadFile(thumbnailFile, "thumbnails/", "image/jpeg");
    }

    public CompletableFuture<UploadResult> uploadVideo(File videoFile) {
        return uploadFile(videoFile, "videos/", "video/mp4");
    }

    private CompletableFuture<UploadResult> uploadFile(File file, String prefix, String contentType) {
        String key = prefix + UUID.randomUUID();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(key)
                .contentType(contentType)
                .build();

        return s3Client.putObject(putObjectRequest, AsyncRequestBody.fromFile(file))
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
}