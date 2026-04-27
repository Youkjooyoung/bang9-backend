package kr.bang9.order.refund.dto;

import jakarta.validation.constraints.Size;

public record RefundDecisionCommand(
    @Size(max = 500, message = "관리자 메모는 500자 이내로 입력해주세요.")
    String adminNote
) {
}
