package kr.bang9.product.review.dto;

public record ReviewSummary(
    Long productId,
    Double averageRating,
    Integer reviewCount
) {}
