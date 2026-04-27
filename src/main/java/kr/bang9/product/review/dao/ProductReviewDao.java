package kr.bang9.product.review.dao;

import kr.bang9.product.review.domain.ProductReview;
import kr.bang9.product.review.dto.ReviewSummary;
import kr.bang9.product.review.dto.ReviewView;
import kr.bang9.product.review.dto.ReviewableOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ProductReviewDao {

    void insertReview(ProductReview review);

    Optional<Long> findProductIdByOrderItem(
        @Param("userId") Long userId,
        @Param("orderCode") String orderCode,
        @Param("orderItemId") Long orderItemId
    );

    Optional<ReviewView> findViewById(@Param("reviewId") Long reviewId);

    Optional<ReviewView> findViewByOrderItem(@Param("orderItemId") Long orderItemId);

    List<ReviewView> findListByProduct(
        @Param("productId") Long productId,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    long countByProduct(@Param("productId") Long productId);

    List<ReviewView> findListByUser(
        @Param("userId") Long userId,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    long countByUser(@Param("userId") Long userId);

    ReviewSummary findSummaryByProduct(@Param("productId") Long productId);

    List<ReviewableOrderItem> findReviewableItemsByOrder(
        @Param("userId") Long userId,
        @Param("orderCode") String orderCode
    );

    Optional<ProductReview> findById(@Param("reviewId") Long reviewId);

    void updateReview(
        @Param("reviewId") Long reviewId,
        @Param("rating") Integer rating,
        @Param("content") String content,
        @Param("imageUrl") String imageUrl
    );

    void deleteReview(@Param("reviewId") Long reviewId);
}
