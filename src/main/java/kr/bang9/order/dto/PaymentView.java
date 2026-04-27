package kr.bang9.order.dto;

import java.time.LocalDateTime;

public record PaymentView(
    Long paymentId,
    String impUid,
    String merchantUid,
    String method,
    Integer amount,
    String status,
    LocalDateTime paidAt
) {}
