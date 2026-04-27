package kr.bang9.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemAddRequest(
    @NotNull(message = "상품 ID가 필요합니다.")
    Long productId,

    Long productOptionId,

    @NotNull(message = "수량을 입력해주세요.")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    @Max(value = 999, message = "수량은 999개 이하여야 합니다.")
    Integer quantity,

    Long sourceListingId
) {
}
