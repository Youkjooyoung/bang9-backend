package kr.bang9.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetail(
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
    String descriptionHtml,
    List<String> imageUrls,
    List<ProductOption> options,
    Double averageRating,
    Integer reviewCount
) {
}
