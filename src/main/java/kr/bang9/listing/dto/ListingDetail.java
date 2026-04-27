package kr.bang9.listing.dto;

import java.math.BigDecimal;

public record ListingDetail(
    Long listingId,
    Long hostUserId,
    String hostNickname,
    String title,
    String description,
    String roomType,
    String dealType,
    Integer deposit,
    Integer monthlyRent,
    Integer maintenanceFee,
    BigDecimal areaM2,
    Short floor,
    Short totalFloor,
    Byte roomCount,
    Byte bathroomCount,
    String addressRoad,
    String addressDetail,
    BigDecimal latitude,
    BigDecimal longitude,
    String status,
    String source,
    Integer viewCount,
    String brokerName,
    String brokerPhone,
    String brokerOfficePhone,
    String brokerOfficeName
) {
}
