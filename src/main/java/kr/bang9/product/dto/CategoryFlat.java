package kr.bang9.product.dto;

public record CategoryFlat(
    Integer categoryId,
    Integer parentId,
    String name,
    Integer depth,
    Integer sortOrder
) {
}
