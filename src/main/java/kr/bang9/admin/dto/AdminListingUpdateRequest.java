package kr.bang9.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminListingUpdateRequest(
    @NotBlank
    @Pattern(regexp = "PENDING|ACTIVE|HIDDEN|SOLD", message = "허용되지 않은 상태값입니다.")
    String status,
    String reason
) {
}
