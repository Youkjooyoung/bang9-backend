package kr.bang9.product.review.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReview {

    private Long reviewId;
    private Long productId;
    private Long userId;
    private Long orderItemId;
    private Integer rating;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
}
