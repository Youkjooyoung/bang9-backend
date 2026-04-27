package kr.bang9.order.refund.dto;

import java.time.LocalDateTime;

public record RefundView(
    Long refundId,
    Long orderId,
    String orderCode,
    Long userId,
    String userNickname,
    Integer amount,
    String reason,
    String status,
    Long adminUserId,
    String adminNickname,
    String adminNote,
    LocalDateTime requestedAt,
    LocalDateTime processedAt,
    LocalDateTime completedAt
) {
}
