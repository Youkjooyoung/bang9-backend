package kr.bang9.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void save(Long userId, String refreshToken, long expireMillis) {
        String key = REFRESH_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, refreshToken, Duration.ofMillis(expireMillis));
    }

    public String find(Long userId) {
        return redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);
    }

    public boolean match(Long userId, String refreshToken) {
        String saved = find(userId);
        return saved != null && saved.equals(refreshToken);
    }

    public void delete(Long userId) {
        redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
    }

    public void blacklist(String jti, long expireMillis) {
        String key = BLACKLIST_KEY_PREFIX + jti;
        redisTemplate.opsForValue().set(key, "1", Duration.ofMillis(expireMillis));
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti));
    }
}
