package kr.bang9.admin.controller;

import jakarta.validation.Valid;
import kr.bang9.admin.dto.AdminListingUpdateRequest;
import kr.bang9.admin.dto.AdminListingView;
import kr.bang9.admin.dto.AdminLogView;
import kr.bang9.admin.dto.AdminOrderView;
import kr.bang9.admin.dto.AdminReportUpdateRequest;
import kr.bang9.admin.dto.AdminReportView;
import kr.bang9.admin.dto.AdminUserUpdateRequest;
import kr.bang9.admin.dto.AdminUserView;
import kr.bang9.admin.service.AdminService;
import kr.bang9.common.dto.PageResponse;
import kr.bang9.common.security.AuthPrincipal;
import kr.bang9.external.publicdata.service.PublicDataService;
import kr.bang9.order.refund.dto.RefundDecisionCommand;
import kr.bang9.order.refund.dto.RefundView;
import kr.bang9.order.refund.service.RefundService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final PublicDataService publicDataService;
    private final RefundService refundService;

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserView>> getUsers(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String role,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.findUsers(keyword, status, role, pageable));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserView> getUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.findUser(userId));
    }

    @PatchMapping("/users/{userId}")
    public ResponseEntity<AdminUserView> updateUser(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long userId,
        @RequestBody @Valid AdminUserUpdateRequest request
    ) {
        return ResponseEntity.ok(adminService.updateUser(principal.userId(), userId, request));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<AdminUserView> suspendUser(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long userId
    ) {
        AdminUserUpdateRequest req = new AdminUserUpdateRequest("SUSPENDED", null);
        return ResponseEntity.ok(adminService.updateUser(principal.userId(), userId, req));
    }

    @GetMapping("/listings")
    public ResponseEntity<Page<AdminListingView>> getListings(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.findListings(status, keyword, pageable));
    }

    @PatchMapping("/listings/{listingId}")
    public ResponseEntity<Void> updateListingStatus(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long listingId,
        @RequestBody @Valid AdminListingUpdateRequest request
    ) {
        adminService.updateListingStatus(principal.userId(), listingId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reports")
    public ResponseEntity<Page<AdminReportView>> getReports(
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.findReports(status, pageable));
    }

    @PatchMapping("/reports/{reportId}")
    public ResponseEntity<AdminReportView> updateReport(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long reportId,
        @RequestBody @Valid AdminReportUpdateRequest request
    ) {
        return ResponseEntity.ok(adminService.updateReport(principal.userId(), reportId, request));
    }

    @GetMapping("/logs")
    public ResponseEntity<Page<AdminLogView>> getLogs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "30") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.findLogs(pageable));
    }

    @GetMapping("/orders")
    public ResponseEntity<Page<AdminOrderView>> getOrders(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminService.findOrders(status, keyword, pageable));
    }

    @GetMapping("/refund-requests")
    public ResponseEntity<PageResponse<RefundView>> getRefundRequests(
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(refundService.getAdminList(status, page, size));
    }

    @PostMapping("/refund-requests/{refundId}/approve")
    public ResponseEntity<RefundView> approveRefund(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long refundId,
        @RequestBody(required = false) @Valid RefundDecisionCommand command
    ) {
        return ResponseEntity.ok(refundService.approve(principal.userId(), refundId, command));
    }

    @PostMapping("/refund-requests/{refundId}/reject")
    public ResponseEntity<RefundView> rejectRefund(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable Long refundId,
        @RequestBody(required = false) @Valid RefundDecisionCommand command
    ) {
        return ResponseEntity.ok(refundService.reject(principal.userId(), refundId, command));
    }

    @PostMapping("/publicdata/crawl")
    public ResponseEntity<Integer> runPublicDataCrawl(
        @RequestParam(required = false) String regionCode,
        @RequestParam(required = false) String yearMonth
    ) {
        int count = (regionCode != null && yearMonth != null)
            ? publicDataService.crawlRegion(regionCode, yearMonth)
            : publicDataService.crawlDefaults();
        return ResponseEntity.ok(count);
    }
}
