package kr.bang9.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import kr.bang9.user.dao.UserDao;
import kr.bang9.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    public static final String SESSION_LINK_USER_ID = "LINK_USER_ID";
    public static final String SESSION_LINK_SUCCESS = "LINK_SUCCESS";

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final UserDao userDao;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(request);
        String provider = request.getClientRegistration().getRegistrationId().toUpperCase();
        OAuthAttributes attrs = OAuthAttributes.from(provider, oauth2User.getAttributes());

        Long linkUserId = getLinkUserIdFromSession();
        if (linkUserId != null) {
            return handleLinkMode(linkUserId, provider, attrs, oauth2User);
        }

        User user = userDao.findByOauth(provider, attrs.providerId())
                .or(() -> autoLinkByEmail(provider, attrs))
                .orElseGet(() -> registerNewUser(provider, attrs));

        userDao.updateLastLoginAt(user.getUserId(), LocalDateTime.now());
        return buildOAuth2User(user, oauth2User.getAttributes());
    }

    private OAuth2User handleLinkMode(Long linkUserId, String provider, OAuthAttributes attrs, OAuth2User oauth2User) {
        Optional<User> alreadyLinked = userDao.findByOauth(provider, attrs.providerId());
        if (alreadyLinked.isPresent() && !alreadyLinked.get().getUserId().equals(linkUserId)) {
            log.warn("소셜 계정 이미 다른 유저에 연동됨 provider={} targetUserId={} linkedUserId={}",
                    provider, linkUserId, alreadyLinked.get().getUserId());
            markLinkError();
            User existingUser = userDao.findById(linkUserId).orElseThrow();
            return buildOAuth2User(existingUser, oauth2User.getAttributes());
        }

        userDao.linkOauth(linkUserId, provider, attrs.providerId());
        markLinkSuccess();
        log.info("소셜 계정 수동 연동 완료 provider={} userId={}", provider, linkUserId);

        User user = userDao.findById(linkUserId).orElseThrow();
        return buildOAuth2User(user, oauth2User.getAttributes());
    }

    private Optional<User> autoLinkByEmail(String provider, OAuthAttributes attrs) {
        if (attrs.email() == null || attrs.email().isBlank()) return Optional.empty();
        return userDao.findByEmail(attrs.email()).map(existing -> {
            if (existing.getOauthProvider() == null) {
                userDao.linkOauth(existing.getUserId(), provider, attrs.providerId());
                log.info("이메일 매칭 자동 연동 provider={} email={} userId={}", provider, attrs.email(), existing.getUserId());
                return userDao.findById(existing.getUserId()).orElse(existing);
            }
            log.info("이메일 매칭 로그인 (기존 소셜 유지) newProvider={} existingProvider={} userId={}",
                    provider, existing.getOauthProvider(), existing.getUserId());
            return existing;
        });
    }

    private User registerNewUser(String provider, OAuthAttributes attrs) {
        String email = attrs.email() != null ? attrs.email()
                : provider.toLowerCase() + "_" + attrs.providerId() + "@oauth.local";
        String nickname = attrs.nickname() != null ? attrs.nickname()
                : provider + "_" + UUID.randomUUID().toString().substring(0, 6);

        User user = User.builder()
                .email(email)
                .passwordHash("")
                .nickname(nickname)
                .profileImageUrl(attrs.profileImageUrl())
                .role("USER")
                .oauthProvider(provider)
                .oauthProviderId(attrs.providerId())
                .status("ACTIVE")
                .build();
        userDao.insertUser(user);
        log.info("신규 OAuth 유저 가입 provider={} email={}", provider, email);
        return user;
    }

    private DefaultOAuth2User buildOAuth2User(User user, Map<String, Object> originalAttrs) {
        Map<String, Object> attrs = new java.util.HashMap<>(originalAttrs);
        attrs.put("app_user_id", user.getUserId());
        attrs.put("app_role", user.getRole());
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole())),
                attrs,
                "app_user_id"
        );
    }

    private Long getLinkUserIdFromSession() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpSession session = attrs.getRequest().getSession(false);
            if (session == null) return null;
            return (Long) session.getAttribute(SESSION_LINK_USER_ID);
        } catch (Exception e) {
            return null;
        }
    }

    private void markLinkSuccess() {
        modifySession(session -> {
            session.removeAttribute(SESSION_LINK_USER_ID);
            session.setAttribute(SESSION_LINK_SUCCESS, "ok");
        });
    }

    private void markLinkError() {
        modifySession(session -> {
            session.removeAttribute(SESSION_LINK_USER_ID);
            session.setAttribute(SESSION_LINK_SUCCESS, "error");
        });
    }

    private void modifySession(java.util.function.Consumer<HttpSession> action) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return;
            HttpServletRequest request = attrs.getRequest();
            HttpSession session = request.getSession(false);
            if (session != null) action.accept(session);
        } catch (Exception ignored) {}
    }
}
