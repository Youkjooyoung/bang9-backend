package kr.bang9.order.payment.dao;

import kr.bang9.order.dto.PaymentView;
import kr.bang9.order.payment.domain.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface PaymentDao {

    void insertPayment(Payment payment);

    Optional<PaymentView> findByOrderId(@Param("orderId") Long orderId);

    Optional<Payment> findByImpUid(@Param("impUid") String impUid);

    void updateStatus(
        @Param("paymentId") Long paymentId,
        @Param("status") String status
    );
}
