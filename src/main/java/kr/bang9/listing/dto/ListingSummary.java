package kr.bang9.listing.dto;

import java.math.BigDecimal;

public record ListingSummary(
    Long listingId,
    String title,
    String roomType,
    String dealType,
    Integer deposit,
    Integer monthlyRent,
    BigDecimal areaM2,
    Byte roomCount,
    Byte bathroomCount,
    String addressRoad,
    BigDecimal latitude,
    BigDecimal longitude,
    String coverImageUrl,
    Integer viewCount,
    String status,
    String rejectReason,
    Boolean favorited
) {
}
