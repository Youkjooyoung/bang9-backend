package kr.bang9.order.controller;

import jakarta.validation.Valid;
import kr.bang9.common.dto.PageResponse;
import kr.bang9.common.security.AuthPrincipal;
import kr.bang9.order.dto.OrderCreateRequest;
import kr.bang9.order.dto.OrderCreateResponse;
import kr.bang9.order.dto.OrderDetail;
import kr.bang9.order.dto.OrderListItem;
import kr.bang9.order.dto.PaymentConfirmRequest;
import kr.bang9.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderCreateResponse> createOrder(
        @AuthenticationPrincipal AuthPrincipal principal,
        @Valid @RequestBody OrderCreateRequest request
    ) {
        return ResponseEntity.ok(orderService.createOrder(principal.userId(), request));
    }

    @PostMapping("/{orderCode}/confirm")
    public ResponseEntity<OrderDetail> confirmPayment(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("orderCode") String orderCode,
        @Valid @RequestBody PaymentConfirmRequest request
    ) {
        return ResponseEntity.ok(orderService.confirmPayment(principal.userId(), orderCode, request));
    }

    @GetMapping("/{orderCode}")
    public ResponseEntity<OrderDetail> getDetail(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("orderCode") String orderCode
    ) {
        return ResponseEntity.ok(orderService.getDetail(principal.userId(), orderCode));
    }

    @GetMapping
    public ResponseEntity<PageResponse<OrderListItem>> getList(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(orderService.getList(principal.userId(), page, size));
    }

    @DeleteMapping("/{orderCode}")
    public ResponseEntity<Void> cancelOrder(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("orderCode") String orderCode
    ) {
        orderService.cancelOrder(principal.userId(), orderCode);
        return ResponseEntity.noContent().build();
    }
}
