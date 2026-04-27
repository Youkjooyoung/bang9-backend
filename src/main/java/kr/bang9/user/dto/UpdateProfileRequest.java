package kr.bang9.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
    String nickname,

    @Size(max = 500, message = "프로필 이미지 URL은 500자 이하여야 합니다.")
    String profileImageUrl,

    @Pattern(regexp = "^$|^01[0-9]-\\d{3,4}-\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
    String phone,

    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    @Pattern(regexp = "^$|^[가-힣A-Za-z\\s]+$", message = "이름은 한글/영문만 사용할 수 있습니다.")
    String name,

    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일 형식이 올바르지 않습니다. (YYYY-MM-DD)")
    String birthDate,

    @Pattern(regexp = "^$|^(MALE|FEMALE|OTHER)$", message = "성별 값이 올바르지 않습니다.")
    String gender
) {
}
