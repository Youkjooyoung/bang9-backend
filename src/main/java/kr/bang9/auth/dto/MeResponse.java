package kr.bang9.auth.dto;

import kr.bang9.user.domain.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MeResponse(
    Long userId,
    String email,
    String nickname,
    String name,
    LocalDate birthDate,
    String gender,
    String role,
    String profileImageUrl,
    String phone,
    String status,
    String oauthProvider,
    LocalDateTime lastLoginAt,
    LocalDateTime createdAt
) {
    public static MeResponse from(User user) {
        return new MeResponse(
            user.getUserId(),
            user.getEmail(),
            user.getNickname(),
            user.getName(),
            user.getBirthDate(),
            user.getGender(),
            user.getRole(),
            user.getProfileImageUrl(),
            user.getPhone(),
            user.getStatus(),
            user.getOauthProvider(),
            user.getLastLoginAt(),
            user.getCreatedAt()
        );
    }
}
