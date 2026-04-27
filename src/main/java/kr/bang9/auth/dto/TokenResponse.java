package kr.bang9.auth.dto;

public record TokenResponse(
    String accessToken,
    long expiresIn,
    String tokenType
) {
    public static TokenResponse of(String accessToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, expiresInSeconds, "Bearer");
    }
}
