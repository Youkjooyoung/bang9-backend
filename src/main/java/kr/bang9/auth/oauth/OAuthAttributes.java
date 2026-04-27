package kr.bang9.auth.oauth;

import java.util.Map;

public record OAuthAttributes(
        String provider,
        String providerId,
        String email,
        String nickname,
        String profileImageUrl
) {

    public static OAuthAttributes from(String provider, Map<String, Object> attributes) {
        return switch (provider.toUpperCase()) {
            case "KAKAO" -> fromKakao(attributes);
            case "GOOGLE" -> fromGoogle(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 OAuth 공급자: " + provider);
        };
    }

    private static OAuthAttributes fromKakao(Map<String, Object> attributes) {
        String providerId = String.valueOf(attributes.get("id"));

        Map<String, Object> kakaoAccount = castMap(attributes.get("kakao_account"));
        Map<String, Object> profile = castMap(kakaoAccount != null ? kakaoAccount.get("profile") : null);

        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
        String nickname = profile != null ? (String) profile.get("nickname") : null;
        String profileImageUrl = profile != null ? (String) profile.get("profile_image_url") : null;

        return new OAuthAttributes("KAKAO", providerId, email, nickname, profileImageUrl);
    }

    private static OAuthAttributes fromGoogle(Map<String, Object> attributes) {
        String providerId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String nickname = (String) attributes.get("name");
        String profileImageUrl = (String) attributes.get("picture");

        return new OAuthAttributes("GOOGLE", providerId, email, nickname, profileImageUrl);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }
}
