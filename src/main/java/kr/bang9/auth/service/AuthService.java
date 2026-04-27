package kr.bang9.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import kr.bang9.auth.dto.FindEmailRequest;
import kr.bang9.auth.dto.FindEmailResponse;
import kr.bang9.auth.dto.LoginRequest;
import kr.bang9.auth.dto.LoginResponse;
import kr.bang9.auth.dto.ResetPasswordRequest;
import kr.bang9.auth.dto.SignupRequest;
import kr.bang9.auth.dto.TokenResponse;
import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.common.security.JwtUtil;
import kr.bang9.user.domain.User;
import kr.bang9.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signup(SignupRequest request) {
        userService.findByEmail(request.email()).ifPresent(existing -> {
            if (existing.getOauthProvider() != null) {
                throw new CustomException(ErrorCode.ERR_SOCIAL_ACCOUNT_EXISTS,
                        "SOCIAL_PROVIDER:" + existing.getOauthProvider());
            }
            throw new CustomException(ErrorCode.ERR_EMAIL_DUPLICATE);
        });
        User user = User.builder()
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .nickname(request.nickname())
            .name(request.name())
            .phone(request.phone())
            .birthDate(request.parsedBirthDate())
            .gender(request.gender())
            .role("USER")
            .status("ACTIVE")
            .build();
        userService.createUser(user);
        return user.getUserId();
    }

    @Transactional
    public void mergeSignup(SignupRequest request) {
        User existing = userService.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        if (existing.getOauthProvider() == null) {
            throw new CustomException(ErrorCode.ERR_EMAIL_DUPLICATE);
        }
        existing.setPasswordHash(passwordEncoder.encode(request.password()));
        existing.setNickname(request.nickname());
        existing.setName(request.name());
        existing.setPhone(request.phone());
        existing.setBirthDate(request.parsedBirthDate());
        existing.setGender(request.gender());
        userService.updateProfile(existing);
        userService.updatePassword(existing.getUserId(), passwordEncoder.encode(request.password()));
        log.info("[AuthService] 소셜 계정 통합 완료 userId={} provider={}",
                existing.getUserId(), existing.getOauthProvider());
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userService.findByEmail(request.email())
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_LOGIN_INVALID));
        if (user.getPasswordHash() == null
            || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new CustomException(ErrorCode.ERR_LOGIN_INVALID);
        }

        String accessToken = jwtUtil.generateAccessToken(user.getUserId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId(), user.getRole());
        refreshTokenService.save(user.getUserId(), refreshToken, jwtUtil.getRefreshExpireMillis());
        userService.updateLastLoginAt(user.getUserId());

        TokenResponse token = TokenResponse.of(accessToken, jwtUtil.getAccessExpireMillis() / 1000);
        return new LoginResponse(
            user.getUserId(),
            user.getEmail(),
            user.getNickname(),
            user.getRole(),
            user.getProfileImageUrl(),
            token
        );
    }

    public TokenResponse refresh(String refreshToken) {
        Claims claims = parseRefreshToken(refreshToken);
        long userId = Long.parseLong(claims.getSubject());
        String role = claims.get("role", String.class);

        if (!refreshTokenService.match(userId, refreshToken)) {
            throw new CustomException(ErrorCode.ERR_AUTH_INVALID);
        }

        String newAccessToken = jwtUtil.generateAccessToken(userId, role);
        return TokenResponse.of(newAccessToken, jwtUtil.getAccessExpireMillis() / 1000);
    }

    public void logout(Long userId, String accessJti, long accessExpMillisLeft) {
        refreshTokenService.delete(userId);
        if (accessJti != null && accessExpMillisLeft > 0) {
            refreshTokenService.blacklist(accessJti, accessExpMillisLeft);
        }
    }

    public String getRefreshToken(Long userId) {
        return refreshTokenService.find(userId);
    }

    public FindEmailResponse findEmail(FindEmailRequest request) {
        User user = userService.findByNameAndPhone(request.name(), request.phone())
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        return new FindEmailResponse(maskEmail(user.getEmail()), user.getCreatedAt());
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        User user = userService.findByEmailAndPhone(request.email(), request.phone())
            .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));
        String tempPassword = userService.resetPassword(user.getUserId());
        log.info("[AuthService] 임시 비밀번호 발급 완료. userId={} email={}", user.getUserId(), user.getEmail());
        return tempPassword;
    }

    private String maskEmail(String email) {
        if (email == null) return "";
        int at = email.indexOf('@');
        if (at <= 0) return email;
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) {
            return local.charAt(0) + "*" + domain;
        }
        int visible = Math.max(1, local.length() - 3);
        StringBuilder sb = new StringBuilder(local.substring(0, visible));
        for (int i = visible; i < local.length(); i++) sb.append('*');
        sb.append(domain);
        return sb.toString();
    }

    public long getRefreshExpireMillis() {
        return jwtUtil.getRefreshExpireMillis();
    }

    private Claims parseRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(ErrorCode.ERR_AUTH_INVALID);
        }
        try {
            Claims claims = jwtUtil.parseClaims(refreshToken);
            if (!"refresh".equals(claims.get("type", String.class))) {
                throw new CustomException(ErrorCode.ERR_AUTH_INVALID);
            }
            return claims;
        } catch (JwtException ex) {
            throw new CustomException(ErrorCode.ERR_AUTH_EXPIRED);
        }
    }
}
