package kr.bang9.listing.dto;

import java.math.BigDecimal;
import java.util.List;

public record ListingDetailResponse(
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
    String brokerOfficeName,
    List<String> imageUrls,
    List<String> optionCodes,
    String aiSummary,
    Boolean isFavorite
) {
    /** 인증 여부에 따라 중개사 연락처(PII) 마스킹 후 응답 생성 */
    public static ListingDetailResponse from(
        ListingDetail base,
        List<String> imageUrls,
        List<String> optionCodes,
        String aiSummary,
        boolean isFavorite,
        boolean authenticated
    ) {
        return new ListingDetailResponse(
            base.listingId(), base.hostUserId(), base.hostNickname(),
            base.title(), base.description(), base.roomType(), base.dealType(),
            base.deposit(), base.monthlyRent(), base.maintenanceFee(),
            base.areaM2(), base.floor(), base.totalFloor(),
            base.roomCount(), base.bathroomCount(),
            base.addressRoad(), base.addressDetail(),
            base.latitude(), base.longitude(),
            base.status(), base.source(), base.viewCount(),
            authenticated ? base.brokerName() : null,
            authenticated ? base.brokerPhone() : null,
            authenticated ? base.brokerOfficePhone() : null,
            authenticated ? base.brokerOfficeName() : null,
            imageUrls, optionCodes, aiSummary, isFavorite
        );
    }
}
