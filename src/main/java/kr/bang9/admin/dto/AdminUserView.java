package kr.bang9.admin.dto;

import java.time.LocalDateTime;

public record AdminUserView(
    Long userId,
    String email,
    String nickname,
    String role,
    String status,
    String oauthProvider,
    LocalDateTime lastLoginAt,
    LocalDateTime createdAt
) {
}
