package kr.bang9.order.refund.controller;

import jakarta.validation.Valid;
import kr.bang9.common.dto.PageResponse;
import kr.bang9.common.security.AuthPrincipal;
import kr.bang9.order.refund.dto.RefundRequestCommand;
import kr.bang9.order.refund.dto.RefundView;
import kr.bang9.order.refund.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/{orderCode}/refund-request")
    public ResponseEntity<RefundView> requestRefund(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("orderCode") String orderCode,
        @Valid @RequestBody RefundRequestCommand command
    ) {
        return ResponseEntity.ok(refundService.createRequest(principal.userId(), orderCode, command));
    }

    @GetMapping("/refunds")
    public ResponseEntity<PageResponse<RefundView>> getMyRefunds(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(refundService.getUserList(principal.userId(), page, size));
    }
}
