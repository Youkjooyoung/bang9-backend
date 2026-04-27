package kr.bang9.address.dto;

public record GeocodingResult(
    String addressName,
    String roadAddressName,
    Double latitude,
    Double longitude
) {
}
