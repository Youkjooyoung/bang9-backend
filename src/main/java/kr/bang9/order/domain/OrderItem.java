package kr.bang9.order.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    private Long orderItemId;
    private Long orderId;
    private Long productId;
    private Long productOptionId;
    private String productSnapshot;
    private Integer quantity;
    private Integer unitPrice;
    private Integer lineTotal;
}
