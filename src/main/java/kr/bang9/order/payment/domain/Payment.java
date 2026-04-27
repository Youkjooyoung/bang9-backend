package kr.bang9.order.payment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    private Long paymentId;
    private Long orderId;
    private String impUid;
    private String merchantUid;
    private String method;
    private Integer amount;
    private String status;
    private LocalDateTime paidAt;
    private String rawResponse;
    private LocalDateTime createdAt;
}
