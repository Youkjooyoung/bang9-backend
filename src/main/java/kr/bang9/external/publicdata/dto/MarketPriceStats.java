package kr.bang9.external.publicdata.dto;

public record MarketPriceStats(
    String regionCode,
    String dealType,
    Integer count,
    Long avgDeposit,
    Long avgMonthlyRent,
    Integer minDeposit,
    Integer maxDeposit
) {
}
