package kr.bang9.listing.dto;

import java.math.BigDecimal;

public record ListingClusterRequest(
    BigDecimal swLat,
    BigDecimal swLng,
    BigDecimal neLat,
    BigDecimal neLng,
    Integer zoomLevel,
    String dealType,
    String roomType
) {
    public static final int DONG_LEVEL_MIN = 5;
    public static final int DONG_LEVEL_MAX = 8;
    public static final int GU_LEVEL_MIN = 9;

    public boolean hasBounds() {
        return swLat != null && swLng != null && neLat != null && neLng != null;
    }

    public int safeZoomLevel() {
        if (zoomLevel == null) {
            return 0;
        }
        return zoomLevel;
    }

    public boolean isGuLevel() {
        return safeZoomLevel() >= GU_LEVEL_MIN;
    }

    public boolean isDongLevel() {
        int z = safeZoomLevel();
        return z >= DONG_LEVEL_MIN && z <= DONG_LEVEL_MAX;
    }
}
