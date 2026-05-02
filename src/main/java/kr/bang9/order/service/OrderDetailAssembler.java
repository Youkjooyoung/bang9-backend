package kr.bang9.order.service;

import kr.bang9.order.dao.OrderDao;
import kr.bang9.order.domain.Order;
import kr.bang9.order.dto.OrderDetail;
import kr.bang9.order.dto.OrderItemView;
import kr.bang9.order.dto.PaymentView;
import kr.bang9.order.payment.dao.PaymentDao;
import kr.bang9.order.refund.dao.RefundDao;
import kr.bang9.order.refund.dto.RefundView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderDetailAssembler {

    private final OrderDao orderDao;
    private final PaymentDao paymentDao;
    private final RefundDao refundDao;

    public OrderDetail assemble(Order order) {
        List<OrderItemView> items = orderDao.findItemsByOrder(order.getOrderId());
        PaymentView payment = paymentDao.findByOrderId(order.getOrderId()).orElse(null);
        RefundView refund = refundDao.findLatestByOrder(order.getOrderId())
            .flatMap(latest -> refundDao.findViewById(latest.getRefundId()))
            .orElse(null);
        return new OrderDetail(
            order.getOrderId(),
            order.getOrderCode(),
            order.getStatus(),
            order.getAddressSnapshot(),
            order.getSubtotal(),
            order.getShippingFee(),
            order.getDiscount(),
            order.getTotal(),
            order.getCreatedAt(),
            items,
            payment,
            refund
        );
    }
}
