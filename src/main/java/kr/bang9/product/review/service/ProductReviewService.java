package kr.bang9.product.review.service;

import kr.bang9.common.exception.CustomException;
import kr.bang9.common.dto.PageResponse;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.common.util.ProfanityFilter;
import kr.bang9.order.dao.OrderDao;
import kr.bang9.order.domain.Order;
import kr.bang9.product.review.dao.ProductReviewDao;
import kr.bang9.product.review.domain.ProductReview;
import kr.bang9.product.review.dto.ReviewCreateCommand;
import kr.bang9.product.review.dto.ReviewPageResponse;
import kr.bang9.product.review.dto.ReviewPublicView;
import kr.bang9.product.review.dto.ReviewSummary;
import kr.bang9.product.review.dto.ReviewUpdateCommand;
import kr.bang9.product.review.dto.ReviewView;
import kr.bang9.product.review.dto.ReviewableOrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private static final Set<String> REVIEWABLE_STATUSES = Set.of("PAID", "PREPARING", "SHIPPED", "DELIVERED");

    private final ProductReviewDao productReviewDao;
    private final OrderDao orderDao;
    private final ProfanityFilter profanityFilter;

    @Transactional
    public ReviewView createReview(long userId, String orderCode, ReviewCreateCommand command) {
        if (profanityFilter.containsProfanity(command.content())) {
            throw new CustomException(ErrorCode.ERR_PROFANITY);
        }
        Order order = orderDao.findDetailByCode(userId, orderCode)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        if (!REVIEWABLE_STATUSES.contains(order.getStatus())) {
            throw new CustomException(ErrorCode.ERR_REVIEW_NOT_ALLOWED);
        }

        Long productId = productReviewDao.findProductIdByOrderItem(userId, orderCode, command.orderItemId())
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));

        productReviewDao.findViewByOrderItem(command.orderItemId()).ifPresent(existing -> {
            throw new CustomException(ErrorCode.ERR_REVIEW_ALREADY_EXISTS);
        });

        ProductReview review = ProductReview.builder()
            .productId(productId)
            .userId(userId)
            .orderItemId(command.orderItemId())
            .rating(command.rating())
            .content(command.content())
            .imageUrl(command.imageUrl())
            .build();
        productReviewDao.insertReview(review);

        return productReviewDao.findViewById(review.getReviewId())
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_INTERNAL));
    }

    @Transactional(readOnly = true)
    public ReviewPageResponse<ReviewPublicView> getListByProduct(Long productId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        int offset = safePage * safeSize;
        List<ReviewPublicView> content = productReviewDao.findListByProduct(productId, offset, safeSize).stream()
            .map(ReviewPublicView::from)
            .toList();
        long total = productReviewDao.countByProduct(productId);
        ReviewSummary summary = productReviewDao.findSummaryByProduct(productId);
        return ReviewPageResponse.of(content, safePage, safeSize, total, summary);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewView> getListByUser(long userId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        int offset = safePage * safeSize;
        List<ReviewView> content = productReviewDao.findListByUser(userId, offset, safeSize);
        long total = productReviewDao.countByUser(userId);
        return PageResponse.of(content, safePage, safeSize, total);
    }

    @Transactional(readOnly = true)
    public List<ReviewableOrderItem> getReviewableItems(long userId, String orderCode) {
        Order order = orderDao.findDetailByCode(userId, orderCode)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        if (!REVIEWABLE_STATUSES.contains(order.getStatus())) {
            return List.of();
        }
        return productReviewDao.findReviewableItemsByOrder(userId, orderCode);
    }

    @Transactional(readOnly = true)
    public ReviewSummary getSummary(Long productId) {
        return productReviewDao.findSummaryByProduct(productId);
    }

    @Transactional
    public ReviewView updateReview(long userId, Long reviewId, ReviewUpdateCommand command) {
        if (profanityFilter.containsProfanity(command.content())) {
            throw new CustomException(ErrorCode.ERR_PROFANITY);
        }
        ProductReview review = productReviewDao.findById(reviewId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        if (!review.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ERR_FORBIDDEN);
        }
        productReviewDao.updateReview(reviewId, command.rating(), command.content(), command.imageUrl());
        return productReviewDao.findViewById(reviewId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_INTERNAL));
    }

    @Transactional
    public void deleteReview(long userId, Long reviewId) {
        ProductReview review = productReviewDao.findById(reviewId)
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        if (!review.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ERR_FORBIDDEN);
        }
        productReviewDao.deleteReview(reviewId);
    }
}
