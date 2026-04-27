package kr.bang9.order.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentConfirmRequest(
    @NotBlank(message = "결제 식별자가 필요합니다.")
    String impUid,

    @NotBlank(message = "주문 식별자가 필요합니다.")
    String merchantUid
) {}
