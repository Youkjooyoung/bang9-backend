package kr.bang9.order.domain;

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
public class Order {

    private Long orderId;
    private Long userId;
    private String orderCode;
    private String addressSnapshot;
    private Integer subtotal;
    private Integer shippingFee;
    private Integer discount;
    private Integer total;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
