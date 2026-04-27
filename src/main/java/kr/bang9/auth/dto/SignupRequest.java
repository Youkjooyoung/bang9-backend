package kr.bang9.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SignupRequest(
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
    String email,

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 64, message = "비밀번호는 8~64자 사이여야 합니다.")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]+$",
        message = "비밀번호는 영문/숫자/특수문자를 모두 포함해야 합니다."
    )
    String password,

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2~20자 사이여야 합니다.")
    String nickname,

    @NotBlank(message = "이름은 필수입니다.")
    @Size(min = 2, max = 50, message = "이름은 2~50자여야 합니다.")
    @Pattern(regexp = "^[가-힣A-Za-z\\s]+$", message = "이름은 한글/영문만 사용할 수 있습니다.")
    String name,

    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
    String phone,

    @NotBlank(message = "생년월일은 필수입니다.")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일 형식이 올바르지 않습니다. (YYYY-MM-DD)")
    String birthDate,

    @NotBlank(message = "성별은 필수입니다.")
    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "성별 값이 올바르지 않습니다.")
    String gender
) {

    public LocalDate parsedBirthDate() {
        return LocalDate.parse(birthDate);
    }
}
