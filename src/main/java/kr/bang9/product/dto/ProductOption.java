package kr.bang9.product.dto;

public record ProductOption(
    Long productOptionId,
    String optionType,
    String optionValue,
    Integer additionalPrice,
    Integer stock
) {
}
