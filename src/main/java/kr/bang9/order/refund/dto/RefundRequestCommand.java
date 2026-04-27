package kr.bang9.order.refund.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefundRequestCommand(
    @NotBlank(message = "환불 사유를 입력해주세요.")
    @Size(max = 500, message = "환불 사유는 500자 이내로 입력해주세요.")
    String reason
) {
}
