package kr.bang9.order.refund.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.bang9.admin.dao.AdminDao;
import kr.bang9.admin.domain.AdminLog;
import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.external.portone.PortOneCancellation;
import kr.bang9.external.portone.PortOneClient;
import kr.bang9.order.dao.OrderDao;
import kr.bang9.order.domain.Order;
import kr.bang9.order.dto.OrderItemView;
import kr.bang9.order.payment.dao.PaymentDao;
import kr.bang9.order.payment.domain.Payment;
import kr.bang9.order.refund.dao.RefundDao;
import kr.bang9.order.refund.domain.Refund;
import kr.bang9.order.refund.dto.RefundDecisionCommand;
import kr.bang9.order.refund.dto.RefundRequestCommand;
import kr.bang9.order.refund.dto.RefundView;
import kr.bang9.product.dao.ProductDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private static final String STATUS_REQUESTED = "REQUESTED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private static final String ORDER_STATUS_PAID = "PAID";
    private static final String ORDER_STATUS_REFUNDED = "REFUNDED";

    private static final String PAY_STATUS_CANCELLED = "CANCELLED";

    private static final String TARGET_REFUND = "REFUND";
    private static final String ACTION_REFUND_APPROVE = "REFUND_APPROVE";
    private static final String ACTION_REFUND_REJECT = "REFUND_REJECT";

    private final RefundDao refundDao;
    private final OrderDao orderDao;
    private final PaymentDao paymentDao;
    private final ProductDao productDao;
    private final PortOneClient portOneClient;
    private final AdminDao adminDao;
    private final ObjectMapper objectMapper;

    @Transactional
    public RefundView createRequest(long userId, String orderCode, RefundRequestCommand command) {
        Order order = orderDao.findDetailByCode(userId, orderCode)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        orderDao.lockOrderForUpdate(order.getOrderId());
        if (!ORDER_STATUS_PAID.equals(order.getStatus())) {
            throw new CustomException(ErrorCode.ERR_REFUND_NOT_ALLOWED);
        }
        refundDao.findLatestByOrder(order.getOrderId()).ifPresent(existing -> {
            if (STATUS_REQUESTED.equals(existing.getStatus())
                || STATUS_APPROVED.equals(existing.getStatus())
                || STATUS_COMPLETED.equals(existing.getStatus())) {
                throw new CustomException(ErrorCode.ERR_REFUND_ALREADY_REQUESTED);
            }
        });

        Refund refund = Refund.builder()
            .orderId(order.getOrderId())
            .userId(userId)
            .amount(order.getTotal())
            .reason(command.reason())
            .status(STATUS_REQUESTED)
            .build();
        refundDao.insertRefund(refund);

        return refundDao.findViewById(refund.getRefundId())
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_INTERNAL));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserList(long userId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        int offset = safePage * safeSize;
        List<RefundView> content = refundDao.findListByUser(userId, offset, safeSize);
        long total = refundDao.countByUser(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminList(String status, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        int offset = safePage * safeSize;
        List<RefundView> content = refundDao.findListForAdmin(status, offset, safeSize);
        long total = refundDao.countForAdmin(status);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalElements", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    @Transactional
    public RefundView approve(long adminUserId, Long refundId, RefundDecisionCommand command) {
        RefundView current = refundDao.findViewById(refundId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        if (!STATUS_REQUESTED.equals(current.status())) {
            throw new CustomException(ErrorCode.ERR_REFUND_NOT_ALLOWED, "요청 상태의 환불만 승인할 수 있습니다.");
        }

        Payment payment = paymentDao.findByOrderId(current.orderId())
            .map(view -> Payment.builder()
                .paymentId(view.paymentId())
                .orderId(current.orderId())
                .impUid(view.impUid())
                .merchantUid(view.merchantUid())
                .method(view.method())
                .amount(view.amount())
                .status(view.status())
                .paidAt(view.paidAt())
                .build())
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_PAYMENT_FAILED, "결제 정보를 찾을 수 없습니다."));

        String pgCancellationId = null;
        try {
            PortOneCancellation cancellation = portOneClient.cancelPayment(
                payment.getImpUid(),
                current.amount(),
                current.reason()
            );
            if (!cancellation.isCancelled()) {
                throw new CustomException(ErrorCode.ERR_PAYMENT_FAILED, "PG 결제 취소가 정상 처리되지 않았습니다.");
            }
            pgCancellationId = cancellation.impUid();
        } catch (CustomException e) {
            if (isMockImpUid(payment.getImpUid())) {
                log.warn("Mock 결제 취소로 간주합니다. impUid={}", payment.getImpUid());
                pgCancellationId = payment.getImpUid();
            } else {
                throw e;
            }
        }

        refundDao.updateDecision(
            refundId,
            STATUS_COMPLETED,
            adminUserId,
            command == null ? null : command.adminNote(),
            pgCancellationId
        );
        orderDao.updateStatus(current.orderId(), ORDER_STATUS_REFUNDED);
        paymentDao.updateStatus(payment.getPaymentId(), PAY_STATUS_CANCELLED);
        restoreStock(current.orderId());

        writeLog(adminUserId, ACTION_REFUND_APPROVE, refundId, Map.of(
            "orderCode", current.orderCode(),
            "amount", current.amount(),
            "pgCancellationId", pgCancellationId == null ? "" : pgCancellationId,
            "adminNote", command == null || command.adminNote() == null ? "" : command.adminNote()
        ));

        return refundDao.findViewById(refundId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_INTERNAL));
    }

    @Transactional
    public RefundView reject(long adminUserId, Long refundId, RefundDecisionCommand command) {
        RefundView current = refundDao.findViewById(refundId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        if (!STATUS_REQUESTED.equals(current.status())) {
            throw new CustomException(ErrorCode.ERR_REFUND_NOT_ALLOWED, "요청 상태의 환불만 반려할 수 있습니다.");
        }
        refundDao.updateDecision(
            refundId,
            STATUS_REJECTED,
            adminUserId,
            command == null ? null : command.adminNote(),
            null
        );
        writeLog(adminUserId, ACTION_REFUND_REJECT, refundId, Map.of(
            "orderCode", current.orderCode(),
            "amount", current.amount(),
            "adminNote", command == null || command.adminNote() == null ? "" : command.adminNote()
        ));
        return refundDao.findViewById(refundId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_INTERNAL));
    }

    private boolean isMockImpUid(String impUid) {
        return impUid != null && impUid.startsWith("mock_imp_");
    }

    private void restoreStock(Long orderId) {
        List<OrderItemView> items = orderDao.findItemsByOrder(orderId);
        for (OrderItemView item : items) {
            if (item.productOptionId() != null) {
                productDao.increaseOptionStock(item.productOptionId(), item.quantity());
            } else {
                productDao.increaseProductStock(item.productId(), item.quantity());
            }
        }
    }

    private void writeLog(Long adminUserId, String action, Long refundId, Map<String, Object> payload) {
        try {
            AdminLog logEntry = AdminLog.builder()
                .adminUserId(adminUserId)
                .action(action)
                .targetType(TARGET_REFUND)
                .targetId(refundId)
                .payload(objectMapper.writeValueAsString(payload))
                .build();
            adminDao.insertLog(logEntry);
        } catch (JsonProcessingException e) {
            log.warn("환불 관리자 로그 기록 실패: {}", e.getMessage());
        }
    }
}
