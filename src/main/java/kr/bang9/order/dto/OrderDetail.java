package kr.bang9.order.dto;

import kr.bang9.order.refund.dto.RefundView;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetail(
    Long orderId,
    String orderCode,
    String status,
    String addressSnapshot,
    Integer subtotal,
    Integer shippingFee,
    Integer discount,
    Integer total,
    LocalDateTime createdAt,
    List<OrderItemView> items,
    PaymentView payment,
    RefundView refund
) {}
