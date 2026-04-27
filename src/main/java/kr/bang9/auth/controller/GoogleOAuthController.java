package kr.bang9.auth.controller;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import kr.bang9.auth.dto.GoogleTokenRequest;
import kr.bang9.auth.dto.TokenResponse;
import kr.bang9.auth.oauth.OAuthAttributes;
import kr.bang9.auth.service.RefreshTokenService;
import kr.bang9.common.security.JwtUtil;
import kr.bang9.user.dao.UserDao;
import kr.bang9.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@Slf4j
@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
public class GoogleOAuthController {

    private static final String TOKENINFO_URL =
            "https://oauth2.googleapis.com/tokeninfo?id_token={token}";

    private final UserDao userDao;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> googleToken(
            @Valid @RequestBody GoogleTokenRequest request
    ) {
        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) RestClient.create()
                .get()
                .uri(TOKENINFO_URL, request.idToken())
                .retrieve()
                .body(Map.class);

        if (info == null || info.get("sub") == null) {
            return ResponseEntity.badRequest().build();
        }

        OAuthAttributes attrs = OAuthAttributes.from("GOOGLE", Map.of(
                "sub", String.valueOf(info.get("sub")),
                "email", String.valueOf(info.getOrDefault("email", "")),
                "name", String.valueOf(info.getOrDefault("name", "")),
                "picture", String.valueOf(info.getOrDefault("picture", ""))
        ));

        User user = userDao.findByOauth("GOOGLE", attrs.providerId())
                .or(() -> autoLinkByEmail(attrs))
                .orElseGet(() -> registerNewUser(attrs));

        userDao.updateLastLoginAt(user.getUserId(), LocalDateTime.now());

        String accessToken = jwtUtil.generateAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId(), user.getRole());
        refreshTokenService.save(user.getUserId(), refreshToken, jwtUtil.getRefreshExpireMillis());

        log.info("Google 모바일 로그인 성공 userId={}", user.getUserId());
        return ResponseEntity.ok(TokenResponse.of(accessToken, jwtUtil.getAccessExpireMillis() / 1000));
    }

    private java.util.Optional<User> autoLinkByEmail(OAuthAttributes attrs) {
        if (attrs.email() == null || attrs.email().isBlank()) return java.util.Optional.empty();
        return userDao.findByEmail(attrs.email()).map(existing -> {
            if (existing.getOauthProvider() == null) {
                userDao.linkOauth(existing.getUserId(), "GOOGLE", attrs.providerId());
                log.info("Google 이메일 자동 연동 email={} userId={}", attrs.email(), existing.getUserId());
                return userDao.findById(existing.getUserId()).orElse(existing);
            }
            log.info("Google 이메일 매칭 로그인 (기존 소셜 유지) existingProvider={} userId={}",
                    existing.getOauthProvider(), existing.getUserId());
            return existing;
        });
    }

    private User registerNewUser(OAuthAttributes attrs) {
        String email = attrs.email() != null && !attrs.email().isBlank()
                ? attrs.email()
                : "google_" + attrs.providerId() + "@oauth.local";
        String nickname = attrs.nickname() != null && !attrs.nickname().isBlank()
                ? attrs.nickname()
                : "Google_" + UUID.randomUUID().toString().substring(0, 6);

        User user = User.builder()
                .email(email)
                .passwordHash("")
                .nickname(nickname)
                .profileImageUrl(attrs.profileImageUrl())
                .role("USER")
                .oauthProvider("GOOGLE")
                .oauthProviderId(attrs.providerId())
                .status("ACTIVE")
                .build();
        userDao.insertUser(user);
        log.info("Google 모바일 신규 유저 가입 email={}", email);
        return user;
    }
}
