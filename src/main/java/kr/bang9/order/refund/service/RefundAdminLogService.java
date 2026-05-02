package kr.bang9.order.refund.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.bang9.admin.dao.AdminDao;
import kr.bang9.admin.domain.AdminLog;
import kr.bang9.order.refund.dto.RefundView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundAdminLogService {

    private static final String TARGET_REFUND = "REFUND";
    private static final String ACTION_REFUND_APPROVE = "REFUND_APPROVE";
    private static final String ACTION_REFUND_REJECT = "REFUND_REJECT";

    private final AdminDao adminDao;
    private final ObjectMapper objectMapper;

    public void logApproval(Long adminUserId, Long refundId, RefundView refund, String pgCancellationId, String adminNote) {
        writeLog(adminUserId, ACTION_REFUND_APPROVE, refundId, Map.of(
            "orderCode", refund.orderCode(),
            "amount", refund.amount(),
            "pgCancellationId", pgCancellationId == null ? "" : pgCancellationId,
            "adminNote", adminNote == null ? "" : adminNote
        ));
    }

    public void logRejection(Long adminUserId, Long refundId, RefundView refund, String adminNote) {
        writeLog(adminUserId, ACTION_REFUND_REJECT, refundId, Map.of(
            "orderCode", refund.orderCode(),
            "amount", refund.amount(),
            "adminNote", adminNote == null ? "" : adminNote
        ));
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
