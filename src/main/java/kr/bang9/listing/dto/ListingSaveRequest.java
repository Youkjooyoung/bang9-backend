package kr.bang9.listing.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ListingSaveRequest(
    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 150, message = "제목은 150자 이내로 입력해주세요.")
    String title,

    @Size(max = 5000, message = "상세 설명은 5000자 이내로 입력해주세요.")
    String description,

    @NotBlank(message = "방 유형을 선택해주세요.")
    String roomType,

    @NotBlank(message = "거래 유형을 선택해주세요.")
    String dealType,

    @NotNull(message = "보증금을 입력해주세요.")
    @Min(value = 0, message = "보증금은 0원 이상이어야 합니다.")
    Integer deposit,

    @NotNull(message = "월세를 입력해주세요.")
    @Min(value = 0, message = "월세는 0원 이상이어야 합니다.")
    Integer monthlyRent,

    @Min(value = 0, message = "관리비는 0원 이상이어야 합니다.")
    Integer maintenanceFee,

    @NotNull(message = "전용 면적을 입력해주세요.")
    @DecimalMin(value = "0.1", message = "전용 면적은 0.1㎡ 이상이어야 합니다.")
    @DecimalMax(value = "9999.99", message = "전용 면적이 올바르지 않습니다.")
    BigDecimal areaM2,

    @Min(value = -100, message = "층수가 올바르지 않습니다.")
    @Max(value = 500, message = "층수가 올바르지 않습니다.")
    Short floor,

    @Min(value = 1, message = "총 층수가 올바르지 않습니다.")
    @Max(value = 500, message = "총 층수가 올바르지 않습니다.")
    Short totalFloor,

    @Min(value = 1, message = "방 개수는 1개 이상이어야 합니다.")
    @Max(value = 20, message = "방 개수가 올바르지 않습니다.")
    Byte roomCount,

    @Min(value = 1, message = "화장실 개수는 1개 이상이어야 합니다.")
    @Max(value = 10, message = "화장실 개수가 올바르지 않습니다.")
    Byte bathroomCount,

    @NotBlank(message = "도로명 주소를 입력해주세요.")
    @Size(max = 255, message = "도로명 주소는 255자 이내로 입력해주세요.")
    String addressRoad,

    @Size(max = 255, message = "상세 주소는 255자 이내로 입력해주세요.")
    String addressDetail,

    @NotNull(message = "위도 값이 필요합니다.")
    @DecimalMin(value = "-90.0", message = "위도가 올바르지 않습니다.")
    @DecimalMax(value = "90.0", message = "위도가 올바르지 않습니다.")
    BigDecimal latitude,

    @NotNull(message = "경도 값이 필요합니다.")
    @DecimalMin(value = "-180.0", message = "경도가 올바르지 않습니다.")
    @DecimalMax(value = "180.0", message = "경도가 올바르지 않습니다.")
    BigDecimal longitude,

    @Size(max = 100, message = "중개사 이름은 100자 이내로 입력해주세요.")
    String brokerName,

    @Size(max = 30, message = "연락처는 30자 이내로 입력해주세요.")
    String brokerPhone,

    @Size(max = 30, message = "사무실 전화번호는 30자 이내로 입력해주세요.")
    String brokerOfficePhone,

    @Size(max = 150, message = "중개 사무소명은 150자 이내로 입력해주세요.")
    String brokerOfficeName,

    List<String> imageUrls,

    List<String> optionCodes
) {
}
