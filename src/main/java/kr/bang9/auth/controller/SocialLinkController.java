package kr.bang9.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kr.bang9.auth.dto.GoogleLinkRequest;
import kr.bang9.auth.oauth.CustomOAuth2UserService;
import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.common.security.AuthPrincipal;
import kr.bang9.common.security.JwtUtil;
import kr.bang9.user.dao.UserDao;
import kr.bang9.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/users/me/social-link")
@RequiredArgsConstructor
public class SocialLinkController {

    private static final String TOKENINFO_URL =
            "https://oauth2.googleapis.com/tokeninfo?id_token={token}";

    private final UserDao userDao;
    private final JwtUtil jwtUtil;

    @PostMapping("/google")
    public ResponseEntity<Void> linkGoogle(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody GoogleLinkRequest request
    ) {
        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) RestClient.create()
                .get()
                .uri(TOKENINFO_URL, request.idToken())
                .retrieve()
                .body(Map.class);

        if (info == null || info.get("sub") == null) {
            throw new CustomException(ErrorCode.ERR_INVALID_PARAMETER);
        }

        String providerId = String.valueOf(info.get("sub"));

        Optional<User> alreadyLinked = userDao.findByOauth("GOOGLE", providerId);
        if (alreadyLinked.isPresent() && alreadyLinked.get().getUserId() != principal.userId()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        userDao.linkOauth(principal.userId(), "GOOGLE", providerId);
        log.info("Google 수동 연동 완료 userId={}", principal.userId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> unlink(@AuthenticationPrincipal AuthPrincipal principal) {
        User user = userDao.findById(principal.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND));

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        userDao.unlinkOauth(principal.userId());
        log.info("소셜 계정 연동 해제 userId={}", principal.userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/kakao/start")
    public void kakaoLinkStart(
            @RequestParam("token") String token,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        Long userId;
        try {
            userId = Long.parseLong(jwtUtil.parseClaims(token).getSubject());
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        HttpSession session = request.getSession(true);
        session.setAttribute(CustomOAuth2UserService.SESSION_LINK_USER_ID, userId);
        session.setMaxInactiveInterval(300);
        response.sendRedirect("/oauth2/authorization/kakao");
    }
}
