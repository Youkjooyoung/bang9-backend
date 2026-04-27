package kr.bang9.admin.dto;

import java.time.LocalDateTime;

public record AdminOrderView(
    Long orderId,
    String orderCode,
    Long userId,
    String userNickname,
    String userEmail,
    Integer total,
    String status,
    String paymentMethod,
    String paymentStatus,
    String refundStatus,
    LocalDateTime createdAt
) {
}
