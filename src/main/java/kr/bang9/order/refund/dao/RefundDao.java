package kr.bang9.order.refund.dao;

import kr.bang9.order.refund.domain.Refund;
import kr.bang9.order.refund.dto.RefundView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface RefundDao {

    void insertRefund(Refund refund);

    Optional<Refund> findLatestByOrder(@Param("orderId") Long orderId);

    Optional<RefundView> findViewById(@Param("refundId") Long refundId);

    List<RefundView> findListByUser(
        @Param("userId") Long userId,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    long countByUser(@Param("userId") Long userId);

    List<RefundView> findListForAdmin(
        @Param("status") String status,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    long countForAdmin(@Param("status") String status);

    void updateDecision(
        @Param("refundId") Long refundId,
        @Param("status") String status,
        @Param("adminUserId") Long adminUserId,
        @Param("adminNote") String adminNote,
        @Param("pgCancellationId") String pgCancellationId
    );
}
