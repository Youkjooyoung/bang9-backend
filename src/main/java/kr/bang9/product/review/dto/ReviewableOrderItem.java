package kr.bang9.product.review.dto;

public record ReviewableOrderItem(
    Long orderItemId,
    Long productId,
    String productName,
    String coverImageUrl,
    Long reviewId
) {}
