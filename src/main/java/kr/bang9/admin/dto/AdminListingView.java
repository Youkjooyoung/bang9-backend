package kr.bang9.admin.dto;

import java.time.LocalDateTime;

public record AdminListingView(
    Long listingId,
    String title,
    String dealType,
    String status,
    Long ownerUserId,
    String ownerNickname,
    Long priceDeposit,
    Long priceMonthly,
    Integer reportCount,
    LocalDateTime createdAt
) {
}
