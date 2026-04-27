package kr.bang9.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.bang9.cart.dao.CartDao;
import kr.bang9.cart.dto.CartItemView;
import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.external.portone.PortOneClient;
import kr.bang9.external.portone.PortOnePayment;
import kr.bang9.order.dao.OrderDao;
import kr.bang9.order.domain.Order;
import kr.bang9.order.domain.OrderItem;
import kr.bang9.order.dto.AddressSnapshot;
import kr.bang9.order.dto.OrderCreateRequest;
import kr.bang9.order.dto.OrderCreateResponse;
import kr.bang9.order.dto.OrderDetail;
import kr.bang9.order.dto.OrderItemView;
import kr.bang9.order.dto.OrderListItem;
import kr.bang9.order.dto.PaymentConfirmRequest;
import kr.bang9.order.dto.PaymentView;
import kr.bang9.order.payment.dao.PaymentDao;
import kr.bang9.order.payment.domain.Payment;
import kr.bang9.order.refund.dao.RefundDao;
import kr.bang9.product.dao.ProductDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final RefundDao refundDao;
    private final CartDao cartDao;
    private final ProductDao productDao;
    private final PortOneClient portOneClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderCreateResponse createOrder(long userId, OrderCreateRequest request) {
        Long cartId = cartDao.findCartIdByUser(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND, "장바구니가 비어있습니다."));
        List<CartItemView> items = cartDao.findItemsByCart(cartId);
        if (items.isEmpty()) {
            throw new CustomException(ErrorCode.ERR_NOT_FOUND, "장바구니가 비어있습니다.");
        }
        validateStock(items);
        int subtotal = items.stream().mapToInt(CartItemView::lineTotal).sum();
        int shipping = items.stream()
            .mapToInt(i -> i.shippingFee() == null ? 0 : i.shippingFee())
            .max().orElse(0);
        int total = subtotal + shipping;

        String orderCode = generateOrderCode();
        Order order = Order.builder()
            .userId(userId)
            .orderCode(orderCode)
            .addressSnapshot(serializeAddress(request.address()))
            .subtotal(subtotal)
            .shippingFee(shipping)
            .discount(0)
            .total(total)
            .status(STATUS_CREATED)
            .build();
        orderDao.insertOrder(order);

        for (CartItemView item : items) {
            OrderItem orderItem = OrderItem.builder()
                .orderId(order.getOrderId())
                .productId(item.productId())
                .productOptionId(item.productOptionId())
                .productSnapshot(serializeProduct(item))
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
            subtotal,
            shipping,
            0,
            total
        );
    }

    @Transactional
    public OrderDetail confirmPayment(long userId, String orderCode, PaymentConfirmRequest request) {
        Order order = orderDao.findDetailByCode(userId, orderCode)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        if (STATUS_PAID.equals(order.getStatus())) {
            return assembleDetail(order);
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

        List<OrderItemView> orderItems = orderDao.findItemsByOrder(order.getOrderId());
        for (OrderItemView item : orderItems) {
            decreaseStock(item.productId(), item.productOptionId(), item.quantity());
        }

        Payment payment = Payment.builder()
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
        paymentDao.insertPayment(payment);

        orderDao.updateStatus(order.getOrderId(), STATUS_PAID);
        cartDao.findCartIdByUser(userId).ifPresent(cartDao::deleteItemsByCart);

        order.setStatus(STATUS_PAID);
        return assembleDetail(order);
    }

    @Transactional(readOnly = true)
    public OrderDetail getDetail(long userId, String orderCode) {
        Order order = orderDao.findDetailByCode(userId, orderCode)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        return assembleDetail(order);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getList(long userId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        int offset = safePage * safeSize;
        List<OrderListItem> list = orderDao.findListByUser(userId, offset, safeSize);
        long total = orderDao.countByUser(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", list);
        result.put("totalElements", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    @Transactional
    public void cancelOrder(long userId, String orderCode) {
        Order order = orderDao.findDetailByCode(userId, orderCode)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        if (!STATUS_CREATED.equals(order.getStatus())) {
            throw new CustomException(ErrorCode.ERR_INVALID_PARAMETER, "결제 대기 상태에서만 취소할 수 있습니다.");
        }
        orderDao.updateStatus(order.getOrderId(), STATUS_CANCELLED);
    }

    private void validateStock(List<CartItemView> items) {
        for (CartItemView item : items) {
            if (item.productOptionId() != null) {
                int stock = productDao.lockOptionStock(item.productOptionId())
                    .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND, "상품 옵션이 존재하지 않습니다."));
                if (stock < item.quantity()) {
                    throw new CustomException(ErrorCode.ERR_STOCK_NOT_ENOUGH);
                }
            } else {
                int stock = productDao.lockProductStock(item.productId())
                    .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND, "상품이 존재하지 않습니다."));
                if (stock < item.quantity()) {
                    throw new CustomException(ErrorCode.ERR_STOCK_NOT_ENOUGH);
                }
            }
        }
    }

    private void decreaseStock(Long productId, Long productOptionId, int quantity) {
        int affected = productOptionId != null
            ? productDao.decreaseOptionStock(productOptionId, quantity)
            : productDao.decreaseProductStock(productId, quantity);
        if (affected == 0) {
            throw new CustomException(ErrorCode.ERR_STOCK_NOT_ENOUGH);
        }
    }

    private boolean isMockImpUid(String impUid) {
        return impUid != null && impUid.startsWith(MOCK_IMP_PREFIX);
    }

    private OrderDetail confirmMockPayment(long userId, Order order, PaymentConfirmRequest request) {
        log.info("Mock 결제 확인 처리. orderCode={}, impUid={}", order.getOrderCode(), request.impUid());
        List<OrderItemView> orderItems = orderDao.findItemsByOrder(order.getOrderId());
        for (OrderItemView item : orderItems) {
            decreaseStock(item.productId(), item.productOptionId(), item.quantity());
        }
        Payment payment = Payment.builder()
            .orderId(order.getOrderId())
            .impUid(request.impUid())
            .merchantUid(request.merchantUid())
            .method(MOCK_METHOD)
            .amount(order.getTotal())
            .status(PAY_STATUS_PAID)
            .paidAt(LocalDateTime.now())
            .rawResponse("{\"mock\":true}")
            .build();
        paymentDao.insertPayment(payment);
        orderDao.updateStatus(order.getOrderId(), STATUS_PAID);
        cartDao.findCartIdByUser(userId).ifPresent(cartDao::deleteItemsByCart);
        order.setStatus(STATUS_PAID);
        return assembleDetail(order);
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

    private OrderDetail assembleDetail(Order order) {
        List<OrderItemView> items = orderDao.findItemsByOrder(order.getOrderId());
        PaymentView payment = paymentDao.findByOrderId(order.getOrderId()).orElse(null);
        kr.bang9.order.refund.dto.RefundView refund = refundDao.findLatestByOrder(order.getOrderId())
            .flatMap(r -> refundDao.findViewById(r.getRefundId()))
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

    private String generateOrderCode() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "B9" + java.time.LocalDate.now().toString().replace("-", "") + suffix;
    }

    private String buildOrderName(List<CartItemView> items) {
        String first = items.get(0).productName();
        if (items.size() == 1) return first;
        return first + " 외 " + (items.size() - 1) + "건";
    }

    private String serializeAddress(AddressSnapshot address) {
        try {
            return objectMapper.writeValueAsString(address);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.ERR_INTERNAL, "주소 직렬화에 실패했습니다.");
        }
    }

    private String serializeProduct(CartItemView item) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("productId", item.productId());
            snapshot.put("productName", item.productName());
            snapshot.put("brand", item.brand());
            snapshot.put("salePrice", item.salePrice());
            snapshot.put("additionalPrice", item.additionalPrice());
            snapshot.put("optionType", item.optionType());
            snapshot.put("optionValue", item.optionValue());
            snapshot.put("coverImageUrl", item.coverImageUrl());
            snapshot.put("sourceListingId", item.sourceListingId());
            snapshot.put("sourceListingTitle", item.sourceListingTitle());
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.ERR_INTERNAL, "상품 스냅샷 저장에 실패했습니다.");
        }
    }
}
