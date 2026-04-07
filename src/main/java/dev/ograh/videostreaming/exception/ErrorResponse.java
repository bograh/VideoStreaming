package dev.ograh.videostreaming.exception;

public record ErrorResponse(
        int status,
        String errorType,
        String errorMessage,
        String path,
        String timestamp,
        String requestId
) {
}