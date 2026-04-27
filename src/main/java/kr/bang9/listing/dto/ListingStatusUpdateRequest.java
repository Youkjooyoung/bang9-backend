package kr.bang9.listing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ListingStatusUpdateRequest(
    @NotBlank
    @Pattern(regexp = "ACTIVE|HIDDEN|SOLD", message = "허용되지 않은 상태값입니다.")
    String status
) {
}
