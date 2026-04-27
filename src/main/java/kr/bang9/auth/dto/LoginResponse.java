package kr.bang9.auth.dto;

public record LoginResponse(
    Long userId,
    String email,
    String nickname,
    String role,
    String profileImageUrl,
    TokenResponse token
) {
}
