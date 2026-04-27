package kr.bang9.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank(message = "현재 비밀번호는 필수입니다.")
    String currentPassword,

    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다.")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]+$",
        message = "비밀번호는 영문/숫자/특수문자(@$!%*#?&)를 모두 포함해야 합니다."
    )
    String newPassword
) {
}
