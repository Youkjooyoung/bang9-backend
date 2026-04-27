package kr.bang9.order.dto;

public record OrderItemView(
    Long orderItemId,
    Long productId,
    Long productOptionId,
    String productSnapshot,
    Integer quantity,
    Integer unitPrice,
    Integer lineTotal
) {}
