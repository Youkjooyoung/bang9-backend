package kr.bang9.product.dto;

public record ProductSearchRequest(
    String keyword,
    Integer categoryId,
    Integer priceMax,
    Integer priceMin,
    Integer widthMax,
    Integer depthMax,
    Integer heightMax,
    Integer page,
    Integer size,
    String sort
) {
    public int safePage() {
        return page == null || page < 0 ? 0 : page;
    }

    public int safeSize() {
        return size == null || size < 1 || size > 100 ? 20 : size;
    }

    public String safeSort() {
        return sort == null || sort.isBlank() ? "latest" : sort;
    }
}
