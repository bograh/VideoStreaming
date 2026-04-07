package dev.ograh.videostreaming.exception;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponseWithFields {
    private ErrorBody error;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ErrorBody {
        int status;
        private String errorType;
        private String errorMessage;
        private String timestamp;
        private List<ErrorDetail> details;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ErrorDetail {
        private String field;
        private String message;
        private String validationType;
    }
}