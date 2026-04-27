package kr.bang9.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressSnapshot(
    @NotBlank(message = "수령인 이름이 필요합니다.")
    @Size(max = 30, message = "수령인 이름이 너무 깁니다.")
    String receiverName,

    @NotBlank(message = "연락처가 필요합니다.")
    @Size(max = 20, message = "연락처 형식이 올바르지 않습니다.")
    String phone,

    @NotBlank(message = "우편번호가 필요합니다.")
    @Size(max = 10, message = "우편번호 형식이 올바르지 않습니다.")
    String zipCode,

    @NotBlank(message = "주소가 필요합니다.")
    @Size(max = 200, message = "주소가 너무 깁니다.")
    String address1,

    @Size(max = 200, message = "상세주소가 너무 깁니다.")
    String address2,

    @Size(max = 500, message = "요청사항이 너무 깁니다.")
    String memo
) {}
