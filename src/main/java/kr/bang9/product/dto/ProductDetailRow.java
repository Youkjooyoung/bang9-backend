package kr.bang9.product.dto;

import java.math.BigDecimal;

public record ProductDetailRow(
    Long productId,
    Integer categoryId,
    String categoryName,
    String name,
    String brand,
    Integer price,
    Integer salePrice,
    Integer stock,
    String status,
    Short widthCm,
    Short depthCm,
    Short heightCm,
    BigDecimal weightKg,
    Integer shippingFee,
    String descriptionHtml
) {
}
