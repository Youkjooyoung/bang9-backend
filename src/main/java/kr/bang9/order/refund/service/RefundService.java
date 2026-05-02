package kr.bang9.order.refund.service;

import kr.bang9.common.dto.PageResponse;
import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.order.dao.OrderDao;
import kr.bang9.order.domain.Order;
import kr.bang9.order.refund.dao.RefundDao;
import kr.bang9.order.refund.domain.Refund;
import kr.bang9.order.refund.dto.RefundDecisionCommand;
import kr.bang9.order.refund.dto.RefundRequestCommand;
import kr.bang9.order.refund.dto.RefundView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    private final RefundDao refundDao;
    private final OrderDao orderDao;
    private final RefundPaymentCancellationService paymentCancellationService;
    private final RefundStockService stockService;
    private final RefundAdminLogService adminLogService;

    @Transactional
    public RefundView createRequest(long userId, String orderCode, RefundRequestCommand command) {
        Order order = findUserOrder(userId, orderCode);
        orderDao.lockOrderForUpdate(order.getOrderId());
        order = findUserOrder(userId, orderCode);

        if (!ORDER_STATUS_PAID.equals(order.getStatus())) {
            throw new CustomException(ErrorCode.ERR_REFUND_NOT_ALLOWED);
        }
        rejectDuplicateRequest(order.getOrderId());

        Refund refund = Refund.builder()
            .orderId(order.getOrderId())
            .userId(userId)
            .amount(order.getTotal())
            .reason(command.reason())
            .status(STATUS_REQUESTED)
            .build();
        refundDao.insertRefund(refund);

        return findView(refund.getRefundId());
    }

    @Transactional(readOnly = true)
    public PageResponse<RefundView> getUserList(long userId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        int offset = safePage * safeSize;
        List<RefundView> content = refundDao.findListByUser(userId, offset, safeSize);
        long total = refundDao.countByUser(userId);
        return PageResponse.of(content, safePage, safeSize, total);
    }

    @Transactional(readOnly = true)
    public PageResponse<RefundView> getAdminList(String status, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        int offset = safePage * safeSize;
        List<RefundView> content = refundDao.findListForAdmin(status, offset, safeSize);
        long total = refundDao.countForAdmin(status);
        return PageResponse.of(content, safePage, safeSize, total);
    }

    @Transactional
    public RefundView approve(long adminUserId, Long refundId, RefundDecisionCommand command) {
        RefundView current = lockAndFindRequested(refundId, "요청 상태의 환불만 승인할 수 있습니다.");
        String pgCancellationId = paymentCancellationService.cancel(current.orderId(), current.amount(), current.reason());

        refundDao.updateDecision(
            refundId,
            STATUS_COMPLETED,
            adminUserId,
            adminNote(command),
            pgCancellationId
        );
        orderDao.updateStatus(current.orderId(), ORDER_STATUS_REFUNDED);
        stockService.restore(current.orderId());
        adminLogService.logApproval(adminUserId, refundId, current, pgCancellationId, adminNote(command));

        return findView(refundId);
    }

    @Transactional
    public RefundView reject(long adminUserId, Long refundId, RefundDecisionCommand command) {
        RefundView current = lockAndFindRequested(refundId, "요청 상태의 환불만 반려할 수 있습니다.");
        refundDao.updateDecision(
            refundId,
            STATUS_REJECTED,
            adminUserId,
            adminNote(command),
            null
        );
        adminLogService.logRejection(adminUserId, refundId, current, adminNote(command));
        return findView(refundId);
    }

    private Order findUserOrder(long userId, String orderCode) {
        return orderDao.findDetailByCode(userId, orderCode)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
    }

    private RefundView lockAndFindRequested(Long refundId, String message) {
        RefundView current = findViewOrNotFound(refundId);
        orderDao.lockOrderForUpdate(current.orderId());
        current = findViewOrNotFound(refundId);
        if (!STATUS_REQUESTED.equals(current.status())) {
            throw new CustomException(ErrorCode.ERR_REFUND_NOT_ALLOWED, message);
        }
        return current;
    }

    private void rejectDuplicateRequest(Long orderId) {
        refundDao.findLatestByOrder(orderId).ifPresent(existing -> {
            if (STATUS_REQUESTED.equals(existing.getStatus())
                || STATUS_APPROVED.equals(existing.getStatus())
                || STATUS_COMPLETED.equals(existing.getStatus())) {
                throw new CustomException(ErrorCode.ERR_REFUND_ALREADY_REQUESTED);
            }
        });
    }

    private RefundView findView(Long refundId) {
        return refundDao.findViewById(refundId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_INTERNAL));
    }

    private RefundView findViewOrNotFound(Long refundId) {
        return refundDao.findViewById(refundId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
    }

    private String adminNote(RefundDecisionCommand command) {
        return command == null ? null : command.adminNote();
    }
}
