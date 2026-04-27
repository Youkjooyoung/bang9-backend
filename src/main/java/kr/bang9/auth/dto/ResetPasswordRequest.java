package kr.bang9.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    String email,

    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
    String phone
) {
}
