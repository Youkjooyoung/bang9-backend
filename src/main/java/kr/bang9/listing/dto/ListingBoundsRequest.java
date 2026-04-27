package kr.bang9.listing.dto;

import java.math.BigDecimal;

public record ListingBoundsRequest(
    BigDecimal swLat,
    BigDecimal swLng,
    BigDecimal neLat,
    BigDecimal neLng,
    String dealType,
    String roomType,
    Integer limit
) {
    public static final int DEFAULT_LIMIT = 100;
    public static final int MAX_LIMIT = 300;

    public boolean hasBounds() {
        return swLat != null && swLng != null && neLat != null && neLng != null;
    }

    public int safeLimit() {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
