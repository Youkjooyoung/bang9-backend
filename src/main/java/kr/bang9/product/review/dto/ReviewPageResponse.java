package kr.bang9.product.review.dto;

import java.util.List;

public record ReviewPageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    ReviewSummary summary
) {
    public static <T> ReviewPageResponse<T> of(
        List<T> content,
        int page,
        int size,
        long totalElements,
        ReviewSummary summary
    ) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new ReviewPageResponse<>(content, page, size, totalElements, totalPages, summary);
    }
}
