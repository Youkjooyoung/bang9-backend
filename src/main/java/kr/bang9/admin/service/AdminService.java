package kr.bang9.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import kr.bang9.admin.dao.AdminDao;
import kr.bang9.admin.domain.AdminLog;
import kr.bang9.admin.dto.AdminListingUpdateRequest;
import kr.bang9.admin.dto.AdminListingView;
import kr.bang9.admin.dto.AdminLogView;
import kr.bang9.admin.dto.AdminOrderView;
import kr.bang9.admin.dto.AdminReportUpdateRequest;
import kr.bang9.admin.dto.AdminReportView;
import kr.bang9.admin.dto.AdminUserUpdateRequest;
import kr.bang9.admin.dto.AdminUserView;
import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final String TARGET_USER = "USER";
    private static final String TARGET_LISTING = "LISTING";
    private static final String TARGET_REPORT = "REPORT";
    private static final String ACTION_USER_STATUS = "USER_STATUS_CHANGE";
    private static final String ACTION_USER_ROLE = "USER_ROLE_CHANGE";
    private static final String ACTION_LISTING_STATUS = "LISTING_STATUS_CHANGE";
    private static final String ACTION_REPORT_STATUS = "REPORT_STATUS_CHANGE";

    private final AdminDao adminDao;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<AdminUserView> findUsers(String keyword, String status, String role, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();
        List<AdminUserView> rows = adminDao.findUsers(keyword, status, role, offset, size);
        int total = adminDao.countUsers(keyword, status, role);
        return new PageImpl<>(rows, pageable, total);
    }

    @Transactional(readOnly = true)
    public AdminUserView findUser(Long userId) {
        return adminDao.findUserById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
    }

    @Transactional
    public AdminUserView updateUser(Long adminUserId, Long userId, AdminUserUpdateRequest request) {
        AdminUserView current = findUser(userId);
        if (request.status() != null && !request.status().equals(current.status())) {
            adminDao.updateUserStatus(userId, request.status());
            writeLog(adminUserId, ACTION_USER_STATUS, TARGET_USER, userId,
                Map.of("from", current.status(), "to", request.status()));
        }
        if (request.role() != null && !request.role().equals(current.role())) {
            adminDao.updateUserRole(userId, request.role());
            writeLog(adminUserId, ACTION_USER_ROLE, TARGET_USER, userId,
                Map.of("from", current.role(), "to", request.role()));
        }
        return findUser(userId);
    }

    @Transactional(readOnly = true)
    public Page<AdminListingView> findListings(String status, String keyword, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();
        List<AdminListingView> rows = adminDao.findListings(status, keyword, offset, size);
        int total = adminDao.countListings(status, keyword);
        return new PageImpl<>(rows, pageable, total);
    }

    @Transactional
    public void updateListingStatus(Long adminUserId, Long listingId, AdminListingUpdateRequest request) {
        String rejectReason = "ACTIVE".equals(request.status()) ? null : request.reason();
        int updated = adminDao.updateListingStatus(listingId, request.status(), rejectReason);
        if (updated == 0) {
            throw new CustomException(ErrorCode.ERR_NOT_FOUND);
        }
        writeLog(adminUserId, ACTION_LISTING_STATUS, TARGET_LISTING, listingId,
            Map.of("status", request.status(), "reason", rejectReason == null ? "" : rejectReason));
    }

    @Transactional(readOnly = true)
    public Page<AdminReportView> findReports(String status, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();
        List<AdminReportView> rows = adminDao.findReports(status, offset, size);
        int total = adminDao.countReports(status);
        return new PageImpl<>(rows, pageable, total);
    }

    @Transactional
    public AdminReportView updateReport(Long adminUserId, Long reportId, AdminReportUpdateRequest request) {
        AdminReportView report = adminDao.findReportById(reportId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        adminDao.updateReportStatus(reportId, request.status());
        writeLog(adminUserId, ACTION_REPORT_STATUS, TARGET_REPORT, reportId,
            Map.of("from", report.status(), "to", request.status(),
                "comment", request.comment() == null ? "" : request.comment()));
        return adminDao.findReportById(reportId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Page<AdminOrderView> findOrders(String status, String keyword, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();
        List<AdminOrderView> rows = adminDao.findOrders(status, keyword, offset, size);
        int total = adminDao.countOrders(status, keyword);
        return new PageImpl<>(rows, pageable, total);
    }

    @Transactional(readOnly = true)
    public Page<AdminLogView> findLogs(Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();
        List<AdminLogView> rows = adminDao.findLogs(offset, size);
        int total = adminDao.countLogs();
        return new PageImpl<>(rows, pageable, total);
    }

    private void writeLog(Long adminUserId, String action, String targetType, Long targetId, Object payload) {
        AdminLog log = AdminLog.builder()
            .adminUserId(adminUserId)
            .action(action)
            .targetType(targetType)
            .targetId(targetId)
            .payload(serialize(payload))
            .build();
        adminDao.insertLog(log);
    }

    private String serialize(Object payload) {
        if (payload == null) return null;
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.ERR_INTERNAL);
        }
    }
}
