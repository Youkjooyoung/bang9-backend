package kr.bang9.cart.domain;

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
public class CartItem {

    private Long cartItemId;
    private Long cartId;
    private Long productId;
    private Long productOptionId;
    private Integer quantity;
    private Long sourceListingId;
    private LocalDateTime addedAt;
}
