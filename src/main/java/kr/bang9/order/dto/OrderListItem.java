package kr.bang9.order.dto;

import java.time.LocalDateTime;

public record OrderListItem(
    Long orderId,
    String orderCode,
    String status,
    Integer total,
    Integer itemCount,
    String firstProductName,
    String firstCoverImage,
    LocalDateTime createdAt
) {}
