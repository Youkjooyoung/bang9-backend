package kr.bang9.listing.dto;

import java.math.BigDecimal;

public record ListingClusterResponse(
    String regionCode,
    String regionName,
    String regionType,
    BigDecimal centerLat,
    BigDecimal centerLng,
    Integer count
) {
    public static final String REGION_TYPE_GU = "GU";
    public static final String REGION_TYPE_DONG = "DONG";
}
