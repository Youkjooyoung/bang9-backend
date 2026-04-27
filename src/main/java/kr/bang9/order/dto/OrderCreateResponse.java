package kr.bang9.order.dto;

public record OrderCreateResponse(
    Long orderId,
    String orderCode,
    String merchantUid,
    String orderName,
    Integer subtotal,
    Integer shippingFee,
    Integer discount,
    Integer total
) {}
