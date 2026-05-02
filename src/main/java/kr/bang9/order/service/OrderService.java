package kr.bang9.order.service;

import kr.bang9.cart.dao.CartDao;
import kr.bang9.cart.dto.CartItemView;
import kr.bang9.common.dto.PageResponse;
import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.external.portone.PortOneClient;
import kr.bang9.external.portone.PortOnePayment;
import kr.bang9.order.dao.OrderDao;
import kr.bang9.order.domain.Order;
import kr.bang9.order.domain.OrderItem;
import kr.bang9.order.dto.OrderCreateRequest;
import kr.bang9.order.dto.OrderCreateResponse;
import kr.bang9.order.dto.OrderDetail;
import kr.bang9.order.dto.OrderItemView;
import kr.bang9.order.dto.OrderListItem;
import kr.bang9.order.dto.PaymentConfirmRequest;
import kr.bang9.order.payment.dao.PaymentDao;
import kr.bang9.order.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String PAY_STATUS_PAID = "PAID";
    private static final String PAY_STATUS_FAILED = "FAILED";
    private static final String MOCK_IMP_PREFIX = "mock_imp_";
    private static final String MOCK_METHOD = "MOCK";
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final OrderDao orderDao;
    private final PaymentDao paymentDao;
    private final CartDao cartDao;
    private final PortOneClient portOneClient;
    private final OrderSnapshotSerializer snapshotSerializer;
    private final OrderStockService stockService;
    private final OrderDetailAssembler detailAssembler;

    @Transactional
    public OrderCreateResponse createOrder(long userId, OrderCreateRequest request) {
        Long cartId = cartDao.findCartIdByUser(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND, "장바구니가 비어있습니다."));
        List<CartItemView> items = cartDao.findItemsByCart(cartId);
        if (items.isEmpty()) {
            throw new CustomException(ErrorCode.ERR_NOT_FOUND, "장바구니가 비어있습니다.");
        }
        stockService.validateStock(items);
        OrderAmounts amounts = OrderAmounts.from(items);

        String orderCode = generateOrderCode();
        Order order = Order.builder()
            .userId(userId)
            .orderCode(orderCode)
            .addressSnapshot(snapshotSerializer.address(request.address()))
            .subtotal(amounts.subtotal())
            .shippingFee(amounts.shipping())
            .discount(0)
            .total(amounts.total())
            .status(STATUS_CREATED)
            .build();
        orderDao.insertOrder(order);

        for (CartItemView item : items) {
            OrderItem orderItem = OrderItem.builder()
                .orderId(order.getOrderId())
                .productId(item.productId())
                .productOptionId(item.productOptionId())
                .productSnapshot(snapshotSerializer.product(item))
                .quantity(item.quantity())
                .unitPrice(item.unitPrice())
                .lineTotal(item.lineTotal())
                .build();
            orderDao.insertOrderItem(orderItem);
        }

        return new OrderCreateResponse(
            order.getOrderId(),
            orderCode,
            orderCode,
            buildOrderName(items),
            amounts.subtotal(),
            amounts.shipping(),
            0,
            amounts.total()
        );
    }

    @Transactional
    public OrderDetail confirmPayment(long userId, String orderCode, PaymentConfirmRequest request) {
        Order order = findLockedOrder(userId, orderCode);
        if (STATUS_PAID.equals(order.getStatus())) {
            return detailAssembler.assemble(order);
        }
        if (!orderCode.equals(request.merchantUid())) {
            throw new CustomException(ErrorCode.ERR_PAYMENT_FAILED, "주문 정보가 일치하지 않습니다.");
        }

        if (isMockImpUid(request.impUid())) {
            return confirmMockPayment(userId, order, request);
        }

        PortOnePayment portOne = portOneClient.fetchPayment(request.impUid());
        if (!portOne.isPaid()) {
            recordFailedPayment(order, request, portOne);
            throw new CustomException(ErrorCode.ERR_PAYMENT_FAILED, "결제가 완료되지 않았습니다.");
        }
        if (!orderCode.equals(portOne.merchantUid())) {
            recordFailedPayment(order, request, portOne);
            throw new CustomException(ErrorCode.ERR_PAYMENT_FAILED, "결제 주문번호가 일치하지 않습니다.");
        }
        if (portOne.amount() != order.getTotal()) {
            recordFailedPayment(order, request, portOne);
            throw new CustomException(ErrorCode.ERR_PAYMENT_FAILED, "결제 금액이 일치하지 않습니다.");
        }

        return completePaidOrder(userId, order, paidPayment(order, portOne));
    }

    @Transactional(readOnly = true)
    public OrderDetail getDetail(long userId, String orderCode) {
        Order order = orderDao.findDetailByCode(userId, orderCode)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        return detailAssembler.assemble(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderListItem> getList(long userId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        int offset = safePage * safeSize;
        List<OrderListItem> list = orderDao.findListByUser(userId, offset, safeSize);
        long total = orderDao.countByUser(userId);
        return PageResponse.of(list, safePage, safeSize, total);
    }

    @Transactional
    public void cancelOrder(long userId, String orderCode) {
        Order order = findLockedOrder(userId, orderCode);
        if (!STATUS_CREATED.equals(order.getStatus())) {
            throw new CustomException(ErrorCode.ERR_INVALID_PARAMETER, "결제 대기 상태에서만 취소할 수 있습니다.");
        }
        orderDao.updateStatus(order.getOrderId(), STATUS_CANCELLED);
    }

    private Order findLockedOrder(long userId, String orderCode) {
        Order order = orderDao.findDetailByCode(userId, orderCode)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        orderDao.lockOrderForUpdate(order.getOrderId());
        return orderDao.findDetailByCode(userId, orderCode)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
    }

    private boolean isMockImpUid(String impUid) {
        return impUid != null && impUid.startsWith(MOCK_IMP_PREFIX);
    }

    private OrderDetail confirmMockPayment(long userId, Order order, PaymentConfirmRequest request) {
        log.info("Mock 결제 확인 처리. orderCode={}, impUid={}", order.getOrderCode(), request.impUid());
        return completePaidOrder(userId, order, mockPayment(order, request));
    }

    private OrderDetail completePaidOrder(long userId, Order order, Payment payment) {
        List<OrderItemView> orderItems = orderDao.findItemsByOrder(order.getOrderId());
        stockService.decreaseOrderedItems(orderItems);
        paymentDao.insertPayment(payment);
        orderDao.updateStatus(order.getOrderId(), STATUS_PAID);
        cartDao.findCartIdByUser(userId).ifPresent(cartDao::deleteItemsByCart);
        order.setStatus(STATUS_PAID);
        return detailAssembler.assemble(order);
    }

    private Payment paidPayment(Order order, PortOnePayment portOne) {
        return Payment.builder()
            .orderId(order.getOrderId())
            .impUid(portOne.impUid())
            .merchantUid(portOne.merchantUid())
            .method(portOne.method() == null ? "unknown" : portOne.method())
            .amount(portOne.amount())
            .status(PAY_STATUS_PAID)
            .paidAt(portOne.paidAtEpochSec() > 0
                ? LocalDateTime.ofInstant(Instant.ofEpochSecond(portOne.paidAtEpochSec()), ZONE)
                : LocalDateTime.now())
            .rawResponse(portOne.rawJson())
            .build();
    }

    private Payment mockPayment(Order order, PaymentConfirmRequest request) {
        return Payment.builder()
            .orderId(order.getOrderId())
            .impUid(request.impUid())
            .merchantUid(request.merchantUid())
            .method(MOCK_METHOD)
            .amount(order.getTotal())
            .status(PAY_STATUS_PAID)
            .paidAt(LocalDateTime.now())
            .rawResponse("{\"mock\":true}")
            .build();
    }

    private void recordFailedPayment(Order order, PaymentConfirmRequest request, PortOnePayment portOne) {
        Payment payment = Payment.builder()
            .orderId(order.getOrderId())
            .impUid(request.impUid())
            .merchantUid(request.merchantUid())
            .method(portOne.method() == null ? "unknown" : portOne.method())
            .amount(portOne.amount())
            .status(PAY_STATUS_FAILED)
            .paidAt(null)
            .rawResponse(portOne.rawJson())
            .build();
        try {
            paymentDao.insertPayment(payment);
        } catch (Exception e) {
            log.warn("결제 실패 기록 저장 오류: {}", e.getMessage());
        }
    }

    private String generateOrderCode() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "B9" + java.time.LocalDate.now().toString().replace("-", "") + suffix;
    }

    private String buildOrderName(List<CartItemView> items) {
        String first = items.get(0).productName();
        if (items.size() == 1) return first;
        return first + " 외 " + (items.size() - 1) + "건";
    }

    private record OrderAmounts(int subtotal, int shipping, int total) {
        private static OrderAmounts from(List<CartItemView> items) {
            int subtotal = items.stream().mapToInt(CartItemView::lineTotal).sum();
            int shipping = items.stream()
                .mapToInt(item -> item.shippingFee() == null ? 0 : item.shippingFee())
                .max()
                .orElse(0);
            return new OrderAmounts(subtotal, shipping, subtotal + shipping);
        }
    }
}
