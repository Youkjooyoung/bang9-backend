package kr.bang9.cart.controller;

import jakarta.validation.Valid;
import kr.bang9.cart.dto.CartItemAddRequest;
import kr.bang9.cart.dto.CartItemUpdateRequest;
import kr.bang9.cart.dto.CartItemView;
import kr.bang9.cart.dto.CartResponse;
import kr.bang9.cart.service.CartService;
import kr.bang9.common.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<CartItemView> addItem(
        @AuthenticationPrincipal AuthPrincipal principal,
        @Valid @RequestBody CartItemAddRequest request
    ) {
        return ResponseEntity.ok(cartService.add(principal.userId(), request));
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(cartService.getCart(principal.userId()));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> getItemCount(
        @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(Map.of("count", cartService.getItemCount(principal.userId())));
    }

    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<Void> updateQuantity(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("cartItemId") Long cartItemId,
        @Valid @RequestBody CartItemUpdateRequest request
    ) {
        cartService.updateQuantity(principal.userId(), cartItemId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<Void> removeItem(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("cartItemId") Long cartItemId
    ) {
        cartService.removeItem(principal.userId(), cartItemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(@AuthenticationPrincipal AuthPrincipal principal) {
        cartService.clear(principal.userId());
        return ResponseEntity.noContent().build();
    }
}
