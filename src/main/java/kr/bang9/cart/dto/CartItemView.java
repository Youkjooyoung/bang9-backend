package kr.bang9.cart.dto;

public record CartItemView(
    Long cartItemId,
    Long productId,
    String productName,
    String brand,
    Integer salePrice,
    Integer shippingFee,
    Integer stock,
    String coverImageUrl,
    Long productOptionId,
    String optionType,
    String optionValue,
    Integer additionalPrice,
    Integer quantity,
    Long sourceListingId,
    String sourceListingTitle
) {
    public int unitPrice() {
        int additional = additionalPrice == null ? 0 : additionalPrice;
        return salePrice + additional;
    }

    public int lineTotal() {
        return unitPrice() * (quantity == null ? 0 : quantity);
    }
}
