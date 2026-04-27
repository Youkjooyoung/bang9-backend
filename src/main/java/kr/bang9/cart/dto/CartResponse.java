package kr.bang9.cart.dto;

import java.util.List;

public record CartResponse(
    Long cartId,
    List<CartItemView> items,
    int itemCount,
    int subtotal,
    int shippingFeeTotal,
    int totalAmount
) {
    public static CartResponse of(Long cartId, List<CartItemView> items) {
        int subtotal = items.stream().mapToInt(CartItemView::lineTotal).sum();
        int shipping = items.stream()
            .mapToInt(item -> item.shippingFee() == null ? 0 : item.shippingFee())
            .max()
            .orElse(0);
        return new CartResponse(cartId, items, items.size(), subtotal, shipping, subtotal + shipping);
    }
}
