package kr.bang9.product.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewCreateCommand(
    @NotNull(message = "주문 상품 식별자가 필요합니다.")
    Long orderItemId,

    @NotNull(message = "별점을 입력해주세요.")
    @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 5점 이하여야 합니다.")
    Integer rating,

    @NotBlank(message = "리뷰 내용을 입력해주세요.")
    @Size(max = 300, message = "리뷰 내용은 300자까지 입력할 수 있습니다.")
    String content,

    String imageUrl
) {}
