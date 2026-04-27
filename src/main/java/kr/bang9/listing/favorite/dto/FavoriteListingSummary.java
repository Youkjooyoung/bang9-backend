package kr.bang9.listing.favorite.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FavoriteListingSummary(
    Long listingId,
    String title,
    String roomType,
    String dealType,
    Integer deposit,
    Integer monthlyRent,
    String addressRoad,
    BigDecimal areaM2,
    String thumbnailUrl,
    String status,
    LocalDateTime favoritedAt
) {
}
