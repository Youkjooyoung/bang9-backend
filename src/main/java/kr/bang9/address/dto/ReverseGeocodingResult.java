package kr.bang9.address.dto;

public record ReverseGeocodingResult(
    String address,
    String regionDepth1,
    String regionDepth2,
    String regionDepth3,
    Double latitude,
    Double longitude
) {
}
