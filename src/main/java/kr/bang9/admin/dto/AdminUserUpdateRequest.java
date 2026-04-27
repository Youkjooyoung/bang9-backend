package kr.bang9.admin.dto;

import jakarta.validation.constraints.Pattern;

public record AdminUserUpdateRequest(
    @Pattern(regexp = "ACTIVE|SUSPENDED|DELETED", message = "허용되지 않은 상태값입니다.") String status,
    @Pattern(regexp = "USER|ADMIN", message = "허용되지 않은 역할입니다.") String role
) {
}
