package kr.bang9.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FindEmailRequest(
    @NotBlank(message = "이름은 필수입니다.")
    @Size(min = 2, max = 50, message = "이름은 2~50자여야 합니다.")
    String name,

    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
    String phone
) {
}
