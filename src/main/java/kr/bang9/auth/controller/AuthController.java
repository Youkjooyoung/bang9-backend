package kr.bang9.auth.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kr.bang9.auth.dto.FindEmailRequest;
import kr.bang9.auth.dto.FindEmailResponse;
import kr.bang9.auth.dto.LoginRequest;
import kr.bang9.auth.dto.LoginResponse;
import kr.bang9.auth.dto.MeResponse;
import kr.bang9.auth.dto.ResetPasswordRequest;
import kr.bang9.auth.dto.SignupRequest;
import kr.bang9.auth.dto.TokenResponse;
import kr.bang9.auth.service.AuthService;
import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.common.security.AuthPrincipal;
import kr.bang9.common.security.JwtUtil;
import kr.bang9.user.domain.User;
import kr.bang9.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final AuthService authService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Long>> signup(@Valid @RequestBody SignupRequest request) {
        Long userId = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("userId", userId));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletResponse response
    ) {
        LoginResponse body = authService.login(request);
        String refreshToken = authService.getRefreshToken(body.userId());
        response.addHeader("Set-Cookie", buildRefreshCookie(refreshToken, authService.getRefreshExpireMillis()).toString());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(HttpServletRequest request) {
        String refreshToken = resolveRefreshCookie(request);
        TokenResponse token = authService.refresh(refreshToken);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @AuthenticationPrincipal AuthPrincipal principal,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.ERR_AUTH_INVALID);
        }
        AccessTokenInfo info = extractAccessTokenInfo(request);
        authService.logout(principal.userId(), info.jti(), info.expireMillisLeft());
        response.addHeader("Set-Cookie", buildRefreshCookie("", 0).toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal AuthPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.ERR_AUTH_INVALID);
        }
        User user = userService.getUser(principal.userId());
        return ResponseEntity.ok(MeResponse.from(user));
    }

    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam("email") String email) {
        java.util.Optional<User> found = userService.findByEmail(email);
        if (found.isEmpty()) return ResponseEntity.ok(Map.of("duplicated", false));
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("duplicated", true);
        body.put("provider", found.get().getOauthProvider());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/signup/merge")
    public ResponseEntity<Void> signupMerge(@Valid @RequestBody SignupRequest request) {
        authService.mergeSignup(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/find-email")
    public ResponseEntity<FindEmailResponse> findEmail(@Valid @RequestBody FindEmailRequest request) {
        return ResponseEntity.ok(authService.findEmail(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String tempPassword = authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("tempPassword", tempPassword));
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

    private String resolveRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new CustomException(ErrorCode.ERR_AUTH_INVALID);
        }
        for (Cookie cookie : cookies) {
            if (REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        throw new CustomException(ErrorCode.ERR_AUTH_INVALID);
    }

    private AccessTokenInfo extractAccessTokenInfo(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer == null || !bearer.startsWith("Bearer ")) {
            return new AccessTokenInfo(null, 0L);
        }
        String token = bearer.substring(7);
        try {
            Claims claims = jwtUtil.parseClaims(token);
            long expireMillisLeft = claims.getExpiration().getTime() - System.currentTimeMillis();
            return new AccessTokenInfo(claims.getId(), Math.max(expireMillisLeft, 0L));
        } catch (JwtException e) {
            return new AccessTokenInfo(null, 0L);
        }
    }

    private record AccessTokenInfo(String jti, long expireMillisLeft) {
    }
}
