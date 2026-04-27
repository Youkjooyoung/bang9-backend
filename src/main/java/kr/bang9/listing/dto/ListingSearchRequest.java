package kr.bang9.listing.dto;

import java.math.BigDecimal;

public record ListingSearchRequest(
    String keyword,
    String dealType,
    String roomType,
    Integer depositMax,
    Integer monthlyRentMax,
    BigDecimal areaMin,
    BigDecimal areaMax,
    BigDecimal swLat,
    BigDecimal swLng,
    BigDecimal neLat,
    BigDecimal neLng,
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
