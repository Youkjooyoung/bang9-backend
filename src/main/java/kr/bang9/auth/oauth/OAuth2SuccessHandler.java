package kr.bang9.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import kr.bang9.auth.service.RefreshTokenService;
import kr.bang9.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.oauth2.success-redirect-url:http://localhost:5180/oauth/success}")
    private String successRedirectUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        Long userId = ((Number) principal.getAttribute("app_user_id")).longValue();
        String role = principal.getAttribute("app_role");

        jakarta.servlet.http.HttpSession session = request.getSession(false);
        String linkResult = session != null ? (String) session.getAttribute(CustomOAuth2UserService.SESSION_LINK_SUCCESS) : null;

        if (linkResult != null) {
            if (session != null) session.removeAttribute(CustomOAuth2UserService.SESSION_LINK_SUCCESS);
            String redirectUrl = successRedirectUrl + "?mode=" + ("error".equals(linkResult) ? "link-error" : "link");
            log.info("소셜 계정 연동 결과 userId={} result={}", userId, linkResult);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return;
        }

        String accessToken = jwtUtil.generateAccessToken(userId, role);
        String refreshToken = jwtUtil.generateRefreshToken(userId, role);
        long refreshExpireMillis = jwtUtil.getRefreshExpireMillis();
        refreshTokenService.save(userId, refreshToken, refreshExpireMillis);

        response.addHeader("Set-Cookie", buildRefreshCookie(refreshToken, refreshExpireMillis).toString());

        String redirectUrl = successRedirectUrl
                + "?accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);

        log.info("OAuth 로그인 성공 userId={} role={}", userId, role);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private ResponseCookie buildRefreshCookie(String value, long maxAgeMillis) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value == null ? "" : value)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ofMillis(maxAgeMillis))
                .build();
    }
}
