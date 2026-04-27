package kr.bang9.product.review.dto;

import java.time.LocalDateTime;

public record ReviewView(
    Long reviewId,
    Long productId,
    Long userId,
    String userNickname,
    Long orderItemId,
    Integer rating,
    String content,
    String imageUrl,
    LocalDateTime createdAt
) {}
