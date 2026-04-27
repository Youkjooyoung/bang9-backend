package kr.bang9.product.dto;

public record ProductSummary(
    Long productId,
    String name,
    String brand,
    Integer price,
    Integer salePrice,
    Integer discountRate,
    String coverImageUrl,
    Integer categoryId,
    String categoryName,
    Short widthCm,
    Short depthCm,
    Short heightCm,
    Double averageRating,
    Integer reviewCount
) {
}
