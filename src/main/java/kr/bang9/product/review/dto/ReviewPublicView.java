package kr.bang9.product.review.dto;

import java.time.LocalDateTime;

public record ReviewPublicView(
    Long reviewId,
    Long productId,
    String maskedNickname,
    Integer rating,
    String content,
    String imageUrl,
    LocalDateTime createdAt
) {

    public static ReviewPublicView from(ReviewView view) {
        return new ReviewPublicView(
            view.reviewId(),
            view.productId(),
            maskNickname(view.userNickname()),
            view.rating(),
            view.content(),
            view.imageUrl(),
            view.createdAt()
        );
    }

    private static String maskNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return "익명";
        }
        int len = nickname.length();
        if (len <= 2) {
            return nickname.charAt(0) + "*";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(nickname.charAt(0));
        for (int i = 1; i < len - 1; i++) {
            sb.append('*');
        }
        sb.append(nickname.charAt(len - 1));
        return sb.toString();
    }
}
