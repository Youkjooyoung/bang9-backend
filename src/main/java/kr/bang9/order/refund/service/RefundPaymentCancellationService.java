package kr.bang9.order.refund.service;

import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.external.portone.PortOneCancellation;
import kr.bang9.external.portone.PortOneClient;
import kr.bang9.order.dto.PaymentView;
import kr.bang9.order.payment.dao.PaymentDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundPaymentCancellationService {

    private static final String PAY_STATUS_CANCELLED = "CANCELLED";

    private final PaymentDao paymentDao;
    private final PortOneClient portOneClient;

    public String cancel(Long orderId, Integer amount, String reason) {
        PaymentView payment = paymentDao.findByOrderId(orderId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_PAYMENT_FAILED, "결제 정보를 찾을 수 없습니다."));

        String pgCancellationId = cancelPgPayment(payment, amount, reason);
        paymentDao.updateStatus(payment.paymentId(), PAY_STATUS_CANCELLED);
        return pgCancellationId;
    }

    private String cancelPgPayment(PaymentView payment, Integer amount, String reason) {
        try {
            PortOneCancellation cancellation = portOneClient.cancelPayment(
                payment.impUid(),
                amount,
                reason
            );
            if (!cancellation.isCancelled()) {
                throw new CustomException(ErrorCode.ERR_PAYMENT_FAILED, "PG 결제 취소가 정상 처리되지 않았습니다.");
            }
            return cancellation.impUid();
        } catch (CustomException e) {
            if (isMockImpUid(payment.impUid())) {
                log.warn("Mock 결제 취소로 간주합니다. impUid={}", payment.impUid());
                return payment.impUid();
            }
            throw e;
        }
    }

    private boolean isMockImpUid(String impUid) {
        return impUid != null && impUid.startsWith("mock_imp_");
    }
}
