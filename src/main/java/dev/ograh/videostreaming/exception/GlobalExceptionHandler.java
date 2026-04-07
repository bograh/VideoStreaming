package dev.ograh.videostreaming.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MultipartProperties multipartProperties;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseWithFields> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorResponseWithFields.ErrorDetail> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponseWithFields.ErrorDetail.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .validationType(resolveValidationType(error))
                        .build())
                .toList();

        ErrorResponseWithFields response = ErrorResponseWithFields.builder()
                .error(ErrorResponseWithFields.ErrorBody.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .errorType("validation_error")
                        .errorMessage("Request validation failed")
                        .timestamp(LocalDateTime.now().toString())
                        .details(details)
                        .build())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex, HttpServletRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(VideoUploadException.class)
    public ResponseEntity<ErrorResponse> handleVideoUploadException(VideoUploadException ex, HttpServletRequest request) {
        log.error("Video upload failed: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException ex, HttpServletRequest request) {
        log.warn("Forbidden request: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTokenException(InvalidTokenException ex, HttpServletRequest request) {
        log.warn("Invalid token request: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource Not Found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(EmailExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailExistsException(EmailExistsException ex, HttpServletRequest request) {
        log.warn("Email exists already: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidVideoException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVideoException(InvalidVideoException ex, HttpServletRequest request) {
        log.warn("Invalid video: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        DataSize maxUploadSize = multipartProperties.getMaxFileSize();

        log.warn("Maximum upload size exceeded. Allowed: {} MB", maxUploadSize.toMegabytes());
        String message = "File exceeds maximum allowed size of " + maxUploadSize.toMegabytes() + "MB";
        return buildErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE, message, request);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        String message = "An unexpected error occurred. Please try again.";
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex, HttpServletRequest request) {
        log.warn("Unauthorized request: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception: {}", ex.getMessage());
        String message = "An unexpected error occurred. Please try again.";
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, request);

    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status, String message, HttpServletRequest request) {

        String requestId = MDC.get("X-Correlation-ID");

        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                LocalDateTime.now().toString(),
                requestId
        );

        return ResponseEntity.status(status).body(errorResponse);
    }

    private String resolveValidationType(FieldError error) {
        String code = error.getCode();

        if (code == null) return "invalid";

        return switch (code) {
            case "Email" -> "invalid_format";
            case "Min", "Max", "Range", "Size" -> "out_of_range";
            case "NotBlank", "NotNull" -> "required";
            default -> "invalid";
        };
    }

}