package kr.bang9.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record OrderCreateRequest(
    @NotNull(message = "배송지 정보가 필요합니다.")
    @Valid
    AddressSnapshot address
) {}
