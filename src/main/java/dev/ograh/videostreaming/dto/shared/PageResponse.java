package dev.ograh.videostreaming.dto.shared;

public record PageResponse<T>(
        T content,
        int pageNumber,
        int pageSize,
        int totalPages,
        long totalElements,
        boolean first,
        boolean last,
        int numberOfElements,
        String sort
) {
}