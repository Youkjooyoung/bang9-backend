package kr.bang9.product.review.controller;

import jakarta.validation.Valid;
import kr.bang9.common.security.AuthPrincipal;
import kr.bang9.product.review.dto.ReviewCreateCommand;
import kr.bang9.product.review.dto.ReviewUpdateCommand;
import kr.bang9.product.review.dto.ReviewView;
import kr.bang9.product.review.dto.ReviewableOrderItem;
import kr.bang9.product.review.service.ProductReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    @PostMapping("/api/orders/{orderCode}/reviews")
    public ResponseEntity<ReviewView> createReview(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("orderCode") String orderCode,
        @Valid @RequestBody ReviewCreateCommand command
    ) {
        return ResponseEntity.ok(
            productReviewService.createReview(principal.userId(), orderCode, command)
        );
    }

    @GetMapping("/api/products/{productId}/reviews")
    public ResponseEntity<Map<String, Object>> getProductReviews(
        @PathVariable("productId") Long productId,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(productReviewService.getListByProduct(productId, page, size));
    }

    @GetMapping("/api/orders/{orderCode}/reviewable-items")
    public ResponseEntity<List<ReviewableOrderItem>> getReviewableItems(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("orderCode") String orderCode
    ) {
        return ResponseEntity.ok(productReviewService.getReviewableItems(principal.userId(), orderCode));
    }

    @GetMapping("/api/users/me/reviews")
    public ResponseEntity<Map<String, Object>> getMyReviews(
        @AuthenticationPrincipal AuthPrincipal principal,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(productReviewService.getListByUser(principal.userId(), page, size));
    }

    @PutMapping("/api/reviews/{reviewId}")
    public ResponseEntity<ReviewView> updateReview(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("reviewId") Long reviewId,
        @Valid @RequestBody ReviewUpdateCommand command
    ) {
        return ResponseEntity.ok(
            productReviewService.updateReview(principal.userId(), reviewId, command)
        );
    }

    @DeleteMapping("/api/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
        @AuthenticationPrincipal AuthPrincipal principal,
        @PathVariable("reviewId") Long reviewId
    ) {
        productReviewService.deleteReview(principal.userId(), reviewId);
        return ResponseEntity.noContent().build();
    }
}
